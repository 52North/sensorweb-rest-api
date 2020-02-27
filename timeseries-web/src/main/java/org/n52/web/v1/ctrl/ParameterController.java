/**
 * Copyright (C) 2013-2020 52°North Initiative for Geospatial Open Source
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
package org.n52.web.v1.ctrl;

import static org.n52.web.v1.ctrl.Stopwatch.startStopwatch;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.n52.io.IoParameters;
import org.n52.io.response.ext.MetadataExtension;
import org.n52.io.v1.data.ParameterOutput;
import org.n52.sensorweb.v1.spi.LocaleAwareSortService;
import org.n52.sensorweb.v1.spi.ParameterService;
import org.n52.sensorweb.v1.spi.ServiceParameterService;
import org.n52.web.BaseController;
import org.n52.web.ResourceNotFoundException;
import org.n52.web.WebExceptionAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@RequestMapping(produces = {"application/json"})
public abstract class ParameterController extends BaseController implements RestfulUrls {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParameterController.class);

    private List<MetadataExtension<ParameterOutput>> metadataExtensions = new ArrayList<>();

    private ParameterService<ParameterOutput> parameterService;
    
    private ServiceParameterService serviceParameterService;

    @RequestMapping(value = "/{item}/extras", method = GET)
    public Map<String, Object> getExtras(@PathVariable("item") String resourceId,
            @RequestParam(required = false) MultiValueMap<String, String> query, final HttpServletResponse response) {
        IoParameters queryMap = preProcess(query, response);
        
        Map<String, Object> extras = new HashMap<>();
        for (MetadataExtension<ParameterOutput> extension : metadataExtensions) {
            ParameterOutput from = parameterService.getParameter(resourceId, queryMap);
            final Map<String, Object> furtherExtras = extension.getExtras(from, queryMap);
            Collection<String> overridableKeys = checkForOverridingData(extras, furtherExtras);
            if ( !overridableKeys.isEmpty()) {
                String[] keys = overridableKeys.toArray(new String[0]);
                LOGGER.warn("Metadata extension overrides existing extra data: {}", Arrays.toString(keys));
            }
            extras.putAll(furtherExtras);
        }
        return extras;
    }

    private Collection<String> checkForOverridingData(Map<String, Object> data, Map<String, Object> dataToAdd) {
        Map<String, Object> currentData = new HashMap<>(data);
        Set<String> overridableKeys = currentData.keySet();
        overridableKeys.retainAll(dataToAdd.keySet());
        return overridableKeys;
    }

    @RequestMapping(method = GET)
    public ModelAndView getCollection(@RequestParam(required=false) MultiValueMap<String, String> query, final HttpServletResponse response) {
        IoParameters queryMap = preProcess(query, response);

        if (queryMap.isExpanded()) {
            Stopwatch stopwatch = startStopwatch();
            ParameterOutput[] result = ParameterController.this.addExtensionInfos(parameterService.getExpandedParameters(queryMap));
            LOGGER.debug("Processing request took {} seconds.", stopwatch.stopInSeconds());

            // TODO remove in v2.0 
            addExtrasForBackwardCompatiblity(result, queryMap);
            
            // TODO add paging

            return new ModelAndView().addObject(result);
        }
        else {
            ParameterOutput[] results = parameterService.getCondensedParameters(queryMap);
            
            // TODO remove in v2.0 
            addExtrasForBackwardCompatiblity(results, queryMap);
            
            // TODO add paging

            return new ModelAndView().addObject(results);
        }
    }

    @RequestMapping(value = "/{item}", method = GET)
    public ModelAndView getItem(@PathVariable("item") String id,
                                @RequestParam(required = false) MultiValueMap<String, String> query, final HttpServletResponse response) {
        IoParameters queryMap = preProcess(query, response);
        ParameterOutput parameter = addExtensionInfo(parameterService.getParameter(id, queryMap));

        if (parameter == null) {
            throw new ResourceNotFoundException("Found no parameter for id '" + id + "'.");
        }
        addExtrasForBackwardCompatiblity(parameter, queryMap);
        
        return new ModelAndView().addObject(parameter);
    }

    protected ParameterOutput[] addExtensionInfos(ParameterOutput[] toBeProcessed) {
        for (ParameterOutput parameterOutput : toBeProcessed) {
            addExtensionInfo(parameterOutput);
        }
        return toBeProcessed;
    }

    protected ParameterOutput addExtensionInfo(ParameterOutput output) {
        for (MetadataExtension<ParameterOutput> extension : metadataExtensions) {
            extension.addExtraMetadataFieldNames(output);
        }
        return output;
    }
    
    @Deprecated
    protected ParameterOutput[] addExtrasForBackwardCompatiblity(ParameterOutput[] toBeProcessed, IoParameters queryMap) {
        for (ParameterOutput parameterOutput : toBeProcessed) {
            addExtrasForBackwardCompatiblity(parameterOutput, queryMap);
        }
        return toBeProcessed;
    }

    @Deprecated
    protected ParameterOutput addExtrasForBackwardCompatiblity(ParameterOutput output, IoParameters queryMap) {
        for (MetadataExtension<ParameterOutput> extension : metadataExtensions) {
            extension.getExtras(output, queryMap);
        }
        return output;
    }

    public ServiceParameterService getServiceParameterService() {
        return serviceParameterService;
    }

    public void setServiceParameterService(ServiceParameterService serviceParameterService) {
        this.serviceParameterService = serviceParameterService;
    }

    public ParameterService<ParameterOutput> getParameterService() {
        return parameterService;
    }

    public void setParameterService(ParameterService<ParameterOutput> parameterService) {
        ParameterService<ParameterOutput> service = new WebExceptionAdapter<>(parameterService);
        this.parameterService = new LocaleAwareSortService<>(service);
    }

    public List<MetadataExtension<ParameterOutput>> getMetadataExtensions() {
        return metadataExtensions;
    }

    public void setMetadataExtensions(List<MetadataExtension<ParameterOutput>> metadataExtensions) {
        this.metadataExtensions = metadataExtensions;
    }
}
