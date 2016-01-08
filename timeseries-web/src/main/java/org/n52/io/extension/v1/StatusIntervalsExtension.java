/**
 * Copyright (C) 2013-2016 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as publishedby the Free
 * Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of the
 * following licenses, the combination of the program with the linked library is
 * not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed under
 * the aforementioned licenses, is permitted by the copyright holders if the
 * distribution is compliant with both the GNU General Public License version 2
 * and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */
package org.n52.io.extension.v1;

import java.io.InputStream;
import java.util.Map;
import java.util.Map.Entry;

import org.n52.io.response.StatusInterval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import org.n52.io.extension.v1.StatusIntervalsExtensionConfig.ConfigInterval;
import org.n52.io.response.ext.MetadataExtension;
import org.n52.io.request.IoParameters;
import org.n52.io.response.v1.SeriesMetadataV1Output;

public class StatusIntervalsExtension extends MetadataExtension<SeriesMetadataV1Output> {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(StatusIntervalsExtension.class);
	
	private static final String CONFIG_FILE = "/config-status-intervals.json";
    
    private static final String EXTENSION_NAME = "statusIntervals";

	private final StatusIntervalsExtensionConfig intervalConfig = readConfig();
	
	private StatusIntervalsExtensionConfig readConfig() {
		try (InputStream config = getClass().getResourceAsStream(CONFIG_FILE);) {
			ObjectMapper om = new ObjectMapper();
			return om.readValue(config, StatusIntervalsExtensionConfig.class);
		} catch (Exception e) {
			LOGGER.error("Could not load {). Using empty config.", CONFIG_FILE, e);
			return new StatusIntervalsExtensionConfig();
		}
	}

    @Override
    public String getExtensionName() {
        return EXTENSION_NAME;
    }

    @Override
    public void addExtraMetadataFieldNames(SeriesMetadataV1Output output) {
        if (hasStatusIntervals(output)) {
            output.addExtra(EXTENSION_NAME);
        }
    }
    
    private boolean hasStatusIntervals(SeriesMetadataV1Output output) {
        return hasSeriesConfiguration(output) || hasPhenomenonConfiguration(output);
    }

    private boolean hasSeriesConfiguration(SeriesMetadataV1Output output) {
        String id = output.getId();
        return intervalConfig.getTimeseriesIntervals().containsKey(id);
    }
	
    private boolean hasPhenomenonConfiguration(SeriesMetadataV1Output output) {
        String id = output.getId();
        return intervalConfig.getPhenomenonIntervals().containsKey(id);
    }

    @Override
    public Map<String, Object> getExtras(SeriesMetadataV1Output output, IoParameters parameters) {
        if ( !hasExtrasToReturn(output, parameters)) {
            return Collections.emptyMap();
        }
        
        if (hasSeriesConfiguration(output)) {
            final StatusInterval[] intervals = createIntervals(getSeriesIntervals(output));
            output.setStatusIntervals(intervals); // stay backwards compatible
            return wrapSingleIntoMap(intervals);
        } else if (hasPhenomenonConfiguration(output)) {
            final StatusInterval[] intervals = createIntervals(getPhenomenonIntervals(output));
            output.setStatusIntervals(intervals); // stay backwards compatible
            return wrapSingleIntoMap(intervals);
        }
        
        LOGGER.error("No status intervals found for {} (id={})", output, output.getId());
        return Collections.emptyMap();
    }
    
    
    private boolean hasExtrasToReturn(SeriesMetadataV1Output output, IoParameters parameters) {
        return super.hasExtrasToReturn(output, parameters)
                && hasStatusIntervals(output);
    }
    
    private StatusIntervalsExtensionConfig.ConfigInterval getSeriesIntervals(SeriesMetadataV1Output output) {
        return intervalConfig.getTimeseriesIntervals().get(output.getId());
    }
    
    private StatusIntervalsExtensionConfig.ConfigInterval getPhenomenonIntervals(SeriesMetadataV1Output output) {
        String id = output.getParameters().getPhenomenon().getId();
        return intervalConfig.getPhenomenonIntervals().get(id);
    }

	private StatusInterval[] createIntervals(ConfigInterval configInterval) {
		Map<String, StatusInterval> statusIntervals = configInterval.getStatusIntervals();
		for (Entry<String, StatusInterval> entry : statusIntervals.entrySet()) {
			StatusInterval value = entry.getValue();
			value.setName(entry.getKey());
		}
		return statusIntervals.values().toArray(new StatusInterval[0]);
	}

}