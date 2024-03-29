/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.wom.api.utils;

import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.meeds.wom.api.constant.WomParsingException;

public class JsonUtils {

  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  static {
    // Workaround when Jackson is defined in shared library with different
    // version and without artifact jackson-datatype-jsr310
    OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    OBJECT_MAPPER.setVisibility(VisibilityChecker.Std.defaultInstance().withFieldVisibility(JsonAutoDetect.Visibility.ANY));
    OBJECT_MAPPER.registerModule(new JavaTimeModule());
    OBJECT_MAPPER.setDateFormat(new StdDateFormat().withTimeZone(TimeZone.getTimeZone("UTC")));
  }

  private JsonUtils() {
    // Utils class
  }

  public static final <T> T fromJsonString(String value, Class<T> resultClass) throws WomParsingException {
    if (StringUtils.isBlank(value)) {
      return null;
    }
    try {
      return OBJECT_MAPPER.readValue(value, resultClass);
    } catch (Exception e) {
      throw new WomParsingException("wom.unableToParseObject", e);
    }
  }

  public static final <T> T fromJsonStringNoCheckedEx(String value, Class<T> resultClass) throws WomParsingException {
    try {
      return fromJsonString(value, resultClass);
    } catch (WomParsingException e) {
      throw new IllegalStateException(e);
    }
  }

  public static final String toJsonString(Object object) throws WomParsingException {
    try {
      return OBJECT_MAPPER.writeValueAsString(object);
    } catch (Exception e) {
      throw new WomParsingException("wom.unableToParseObject", e);
    }
  }

  public static final String toJsonStringNoCheckedEx(Object object) {
    try {
      return toJsonString(object);
    } catch (WomParsingException e) {
      throw new IllegalStateException(e);
    }
  }

}
