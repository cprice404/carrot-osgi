package com.carrotgarden.osgi.anno.scr.make;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Property;
import org.osgi.service.component.annotations.Reference;

import com.carrotgarden.osgi.anno.scr.bean.AggregatorBean;
import com.carrotgarden.osgi.anno.scr.bean.ComponentBean;
import com.carrotgarden.osgi.anno.scr.bean.PropertyBean;
import com.carrotgarden.osgi.anno.scr.bean.PropertyFileBean;
import com.carrotgarden.osgi.anno.scr.bean.ProvideBean;
import com.carrotgarden.osgi.anno.scr.bean.ReferenceBean;
import com.carrotgarden.osgi.anno.scr.bean.ServiceBean;
import com.carrotgarden.osgi.anno.scr.conv.PropertyType;
import com.carrotgarden.osgi.anno.scr.util.Util;

public class Builder {

	private final Set<String> ignoreService;

	public Builder(final Set<String> ignoreService) {
		this.ignoreService = ignoreService;
	}

	public AggregatorBean makeAggregator(final List<Class<?>> klazList) {

		final AggregatorBean aggregator = new AggregatorBean();

		for (final Class<?> klaz : klazList) {
			aggregator.componentList.add(makeComponent(klaz));
		}

		return aggregator;

	}

	private ComponentBean makeComponent(final Class<?> klaz) {

		final ComponentBean bean = new ComponentBean();

		final List<Class<?>> typeList = Util.getInheritanceList(klaz);

		for (final Class<?> type : typeList) {

			applyServiceInferred(bean, type);

			applyPropertyEmbedded(bean, type);

			applyReference(bean, type);

			applyLifecycle(bean, type);

			// keep last
			applyComponent(bean, type);

		}

		filterService(bean);

		return bean;

	}

	private void applyLifecycle(final ComponentBean bean, final Class<?> type) {

		final Method[] methodArray = type.getDeclaredMethods();

		for (final Method method : methodArray) {

			final String methodName = method.getName();

			if (method.getAnnotation(Activate.class) != null) {
				bean.activate = methodName;
			}

			if (method.getAnnotation(Deactivate.class) != null) {
				bean.deactivate = methodName;
			}

			if (method.getAnnotation(Modified.class) != null) {
				bean.modified = methodName;
			}

		}

	}

	private void filterService(final ComponentBean bean) {

		if (bean.service.provideSet.isEmpty()) {
			bean.service = null;
			return;
		}

		if (ignoreService.isEmpty()) {
			return;
		}

		final Set<ProvideBean> filteredSet = new TreeSet<ProvideBean>();

		for (final ProvideBean provide : bean.service.provideSet) {
			if (ignoreService.contains(provide.type)) {
				continue;
			} else {
				filteredSet.add(provide);
			}
		}

		if (filteredSet.isEmpty()) {
			bean.service = null;
		} else {
			bean.service.provideSet = filteredSet;
		}

	}

	private void applyComponent(final ComponentBean bean, final Class<?> type) {

		if (Util.hasComponent(type)) {

			final Component anno = type.getAnnotation(Component.class);

			bean.name = Util.isValid(anno.name()) ? anno.name() : type
					.getName();

			bean.enabled = anno.enabled();

			bean.factory = Util.isValid(anno.factory()) ? anno.factory() : null;

			bean.immediate = anno.immediate();

			bean.policy = anno.configurationPolicy();

			//

			bean.implementation.klaz = type.getName();

			bean.service.servicefactory = anno.servicefactory();

			//

			applyPropertyKeyValue(bean, anno, type);

			applyPropertyFileEntry(bean, anno, type);

			//

			applyServiceDeclared(bean, anno, type);

		}

	}

	private void applyPropertyKeyValue(final ComponentBean bean,
			final Component anno, final Class<?> klaz) {

		for (final String entry : anno.property()) {

			if (!Util.isValid(entry)) {
				throw new IllegalArgumentException(
						"property must not be empty : " + klaz + " / " + anno);
			}

			if (!entry.contains("=")) {
				throw new IllegalArgumentException(
						"property must contain '=' : " + klaz + " / " + anno);
			}

			final int indexEquals = entry.indexOf("=");

			final String nameType = entry.substring(0, indexEquals);

			if (nameType.length() == 0) {
				throw new IllegalArgumentException(
						"property name must not be empty : " + klaz + " / "
								+ anno);
			}

			final String name;
			final String type;

			if (nameType.contains(":")) {

				final int indexColon = nameType.indexOf(":");

				name = nameType.substring(0, indexColon);
				type = PropertyType.from(nameType.substring(indexColon + 1)).value;

				if (name.length() == 0) {
					throw new IllegalArgumentException(
							"property name must not be empty : " + klaz + " / "
									+ anno);
				}

			} else {
				name = nameType;
				type = PropertyType.STRING.value;
			}

			final String value = entry.substring(indexEquals + 1);

			final PropertyBean propBean = new PropertyBean();

			propBean.name = name;
			propBean.type = type;
			propBean.value = value;

			bean.propertySet.remove(propBean);
			bean.propertySet.add(propBean);

		}

	}

	private void applyPropertyFileEntry(final ComponentBean bean,
			final Component anno, final Class<?> type) {

		for (final String entry : anno.properties()) {

			if (!Util.isValid(entry)) {
				throw new IllegalArgumentException(
						"property entry must not be empty : " + type + " / "
								+ anno);
			}

			final PropertyFileBean propBean = new PropertyFileBean();

			propBean.entry = entry;

			bean.propertyFileSet.remove(propBean);
			bean.propertyFileSet.add(propBean);

		}

	}

	private void applyPropertyEmbedded(final ComponentBean component,
			final Class<?> type) {

		if (!Util.hasProperty(type)) {
			return;
		}

		final Field[] fieldArray = type.getDeclaredFields();

		for (final Field field : fieldArray) {

			final Property anno = field.getAnnotation(Property.class);

			if (anno == null) {
				continue;
			}

			field.setAccessible(true);

			final String fieldName = field.getName();

			final int modifiers = field.getModifiers();

			if (!Modifier.isStatic(modifiers)) {
				throw new IllegalArgumentException(
						"property field must be static : " + fieldName);
			}

			if (!Modifier.isFinal(modifiers)) {
				throw new IllegalArgumentException(
						"property field must be final : " + fieldName);
			}

			if (!String.class.equals(field.getType())) {
				throw new IllegalArgumentException(
						"property annotated type is invalid : " + fieldName
								+ " / " + field.getType());
			}

			//

			final String name = Util.isValid(anno.name()) ? anno.name()
					: fieldName;

			final String value;
			try {
				value = (String) field.get(null);
			} catch (final Exception e) {
				throw new IllegalArgumentException(
						"property annotated value is invalid : " + fieldName, e);
			}

			final PropertyBean bean = new PropertyBean();

			bean.name = name;
			bean.type = PropertyType.STRING.value;
			bean.value = value;

			component.propertySet.remove(bean);
			component.propertySet.add(bean);

		}

	}

	private void applyServiceDeclared(final ComponentBean component,
			final Component anno, final Class<?> type) {

		final Class<?>[] serviceArray = anno.service();

		if (serviceArray == null || serviceArray.length == 0) {
			return;
		}

		final Set<ProvideBean> provideSet = component.service.provideSet;

		provideSet.clear();

		for (final Class<?> service : serviceArray) {

			if (!service.isAssignableFrom(type)) {
				throw new IllegalArgumentException(
						"annotated service must also be implemented : "
								+ service + " / " + type);
			}

			final ProvideBean bean = new ProvideBean();

			bean.type = service.getName();

			provideSet.add(bean);

		}

	}

	private void applyServiceInferred(final ComponentBean component,
			final Class<?> type) {

		final Class<?>[] ifaceArray = type.getInterfaces();

		if (ifaceArray.length == 0) {
			return;
		}

		final ServiceBean service = component.service;

		for (final Class<?> iface : ifaceArray) {

			final ProvideBean bean = new ProvideBean();
			bean.type = iface.getName();

			if (service.provideSet.contains(bean)) {
				continue;
			}

			service.provideSet.add(bean);

		}

	}

	private void applyReference(final ComponentBean component,
			final Class<?> type) {

		final Method[] methodArray = type.getDeclaredMethods();

		for (final Method method : methodArray) {

			final Reference anno = method.getAnnotation(Reference.class);

			if (anno == null) {
				continue;
			}

			final Class<?>[] paramArray = method.getParameterTypes();

			if (!Util.isValid(paramArray)) {
				throw new IllegalArgumentException("invalid reference : "
						+ method);
			}

			final ReferenceBean bean = makeReference(type, method, anno);

			final Set<ReferenceBean> referenceSet = component.referenceSet;

			if (referenceSet.contains(bean)) {
				throw new IllegalArgumentException("duplicate reference : "
						+ method);
			}

			referenceSet.add(bean);

		}

	}

	private ReferenceBean makeReference(final Class<?> type,
			final Method bindMethod, final Reference anno) {

		final ReferenceBean bean = new ReferenceBean();

		final String bindName = bindMethod.getName();

		final Class<?> bindType = Util.bindType(bindMethod);

		//

		bean.type = bindType.getName();

		bean.target = Util.isValid(anno.target()) ? anno.target() : null;

		bean.name = Util.isValid(anno.name()) ? anno.name() : bean.type + "/"
				+ bean.target;

		bean.cardinality = anno.cardinality();

		bean.policy = anno.policy();

		bean.bind = bindName;

		bean.unbind = Util.unbindName(bindName);

		//

		Util.assertBindUnbind(type, bindType, bean.bind, bean.unbind);

		return bean;

	}

}