/**
 * Copyright (C) 2010-2012 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.osgi.anno.scr.conv;

import com.carrotgarden.osgi.anno.scr.bean.PropertyBean;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class PropertyBeanConverter implements Converter {

	@Override
	public boolean canConvert(@SuppressWarnings("rawtypes") final Class klaz) {
		return PropertyBean.class.equals(klaz);
	}

	@Override
	public void marshal(final Object value,
			final HierarchicalStreamWriter writer,
			final MarshallingContext context) {

		final PropertyBean bean = (PropertyBean) value;

		writer.addAttribute("name", bean.name);
		writer.addAttribute("type", bean.type);
		writer.setValue(bean.value);

	}

	@Override
	public Object unmarshal(final HierarchicalStreamReader reader,
			final UnmarshallingContext context) {

		final PropertyBean bean = new PropertyBean();

		bean.name = reader.getAttribute("name");
		bean.type = reader.getAttribute("type");
		bean.value = reader.getValue();

		return bean;

	}

}
