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

import static org.n52.io.QueryParameters.createFromQuery;
import static org.n52.web.v1.ctrl.ResourcesController.ResourceCollection.createResource;
import static org.n52.web.v1.ctrl.RestfulUrls.API_VERSION_PATH;

import java.util.ArrayList;
import java.util.List;

import org.n52.sensorweb.v1.spi.CountingMetadataService;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(value = API_VERSION_PATH, produces = {"application/json"})
public final class ResourcesController {

    private CountingMetadataService metadataService;

    @RequestMapping("/")
    public ModelAndView getResources(@RequestParam(required = false) MultiValueMap<String, String> query) {
        return new ModelAndView().addObject(createResources(createFromQuery(query).isExpanded()));
    }

    private ResourceCollection[] createResources(boolean expanded) {
        List<ResourceCollection> resources = new ArrayList<ResourceCollection>();
        ResourceCollection services = createResource("services").withLabel("Service Provider").withDescription("A service provider offers timeseries data.");
        ResourceCollection stations = createResource("stations").withLabel("Station").withDescription("A station is the place where measurement takes place.");
        ResourceCollection timeseries = createResource("timeseries").withLabel("Timeseries").withDescription("Represents a sequence of data values measured over time.");
        ResourceCollection categories = createResource("categories").withLabel("Category").withDescription("A category group available timeseries.");
        ResourceCollection offerings = createResource("offerings").withLabel("Offering").withDescription("An organizing unit to filter resources.");
        ResourceCollection features = createResource("features").withLabel("Feature").withDescription("An organizing unit to filter resources.");
        ResourceCollection procedures = createResource("procedures").withLabel("Procedure").withDescription("An organizing unit to filter resources.");
        ResourceCollection phenomena = createResource("phenomena").withLabel("Phenomenon").withDescription("An organizing unit to filter resources.");
        if (expanded) {
            services.setSize(metadataService.getServiceCount());
            stations.setSize(metadataService.getStationsCount());
            timeseries.setSize(metadataService.getTimeseriesCount());
            categories.setSize(metadataService.getCategoriesCount());
            offerings.setSize(metadataService.getOfferingsCount());
            features.setSize(metadataService.getFeaturesCount());
            procedures.setSize(metadataService.getProceduresCount());
            phenomena.setSize(metadataService.getPhenomenaCount());
        }
        resources.add(services);
        resources.add(stations);
        resources.add(timeseries);
        resources.add(categories);
        resources.add(offerings);
        resources.add(features);
        resources.add(procedures);
        resources.add(phenomena);
        return resources.toArray(new ResourceCollection[0]);
    }

    public CountingMetadataService getMetadataService() {
        return metadataService;
    }

    public void setMetadataService(CountingMetadataService metadataService) {
        this.metadataService = metadataService;
    }

    static class ResourceCollection {

        private String id;
        private String label;
        private String description;
        private Integer size;

        private ResourceCollection(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Integer getSize() {
            return size;
        }

        public void setSize(Integer size) {
            this.size = size;
        }

        public ResourceCollection withLabel(String label) {
            this.label = label;
            return this;
        }

        public ResourceCollection withDescription(String description) {
            this.description = description;
            return this;
        }

        public ResourceCollection withCount(Integer count) {
            this.size = count;
            return this;
        }

        public static ResourceCollection createResource(String id) {
            return new ResourceCollection(id);
        }
    }
}
