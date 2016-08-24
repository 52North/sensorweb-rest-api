/*
 * Copyright (C) 2013-2016 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public License
 * version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package org.n52.io.response.dataset.count;

import java.util.Map;
import java.util.Map.Entry;

import org.n52.io.response.dataset.Data;

public class CountData extends Data<CountValue> {

    private static final long serialVersionUID = -3990317208637642482L;

    private CountDatasetMetadata metadata;

    /**
     * @param values the timestamp &lt;-&gt; value map.
     * @return a measurement data object.
     */
    public static CountData newCountObservationData(Map<Long, Integer> values) {
        CountData timeseries = new CountData();
        for (Entry<Long, Integer> data : values.entrySet()) {
            timeseries.addNewValue(data.getKey(), data.getValue());
        }
        return timeseries;
    }

    public static CountData newCountObservationData(CountValue... values) {
        CountData timeseries = new CountData();
        timeseries.addValues(values);
        return timeseries;
    }

    private void addNewValue(Long timestamp, Integer value) {
        addNewValue(new CountValue(timestamp, value));
    }

    @Override
    public CountDatasetMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(CountDatasetMetadata metadata) {
        this.metadata = metadata;
    }

}
