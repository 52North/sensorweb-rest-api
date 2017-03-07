/**
 * Copyright (C) 2013-2017 52°North Initiative for Geospatial Open Source
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
package org.n52.io.v1.data;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.n52.io.IoParameters;

public class IOParametersTest {
    
    @Test
    public void testJsonOverriddenWidthConfigParameter() {
        IoParameters parameters = IoParameters.createDefaults();
        Assert.assertThat(parameters.getChartDimension().getWidth(), Matchers.is(2000));
    }
    
    @Test
    public void testBooleanValue() {
        IoParameters parameters = IoParameters.createDefaults();
        Assert.assertTrue(parameters.isGeneralize());
    }
    
    @Test
    public void testAfterConvertedFromParameterSet() {
        UndesignedParameterSet set = UndesignedParameterSet.createForSingleTimeseries("1", IoParameters.createDefaults());
        IoParameters parameters = IoParameters.createFromQuery(set);
        Assert.assertTrue(parameters.isGeneralize());
    }
    
}
