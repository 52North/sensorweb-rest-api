/**
 * Copyright (C) 2013-2015 52°North Initiative for Geospatial Open Source
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
package org.n52.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.servlet.ServletConfig;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.n52.io.format.TvpDataCollection;
import org.n52.io.img.ChartDimension;
import org.n52.io.img.RenderingContext;
import org.n52.io.request.IoParameters;
import static org.n52.io.img.RenderingContext.createContextForSingleTimeseries;
import org.n52.io.PrerenderingJobConfig.ConfiguredStyle;
import org.n52.io.request.RequestSimpleParameterSet;
import org.n52.io.request.StyleProperties;
import org.n52.io.response.TimeseriesMetadataOutput;
import static org.n52.io.request.RequestSimpleParameterSet.createForSingleTimeseries;
import org.n52.io.response.OutputCollection;
import org.n52.io.response.ParameterOutput;
import org.n52.io.task.ScheduledJob;
import org.n52.sensorweb.spi.ParameterService;
import org.n52.sensorweb.spi.SeriesDataService;
import org.n52.web.exception.ResourceNotFoundException;
import org.quartz.InterruptableJob;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ServletConfigAware;

public class PreRenderingJob extends ScheduledJob implements InterruptableJob, ServletConfigAware {

    private final static Logger LOGGER = LoggerFactory.getLogger(PreRenderingJob.class);

    private final PrerenderingJobConfig taskConfigPrerendering;

    private ParameterService<TimeseriesMetadataOutput> timeseriesMetadataService;

    private SeriesDataService timeseriesDataService;

    private String webappFolder;

    private String outputPath;

    private boolean enabled;

    private int width = 800;
    private int height = 500;
    private String language = "en";
    private boolean showGrid = true;

    public PreRenderingJob(String configFile) { // XXX
        taskConfigPrerendering = readJobConfig(configFile);
    }

    private PrerenderingJobConfig readJobConfig(String configFile) {
        
        try (InputStream taskConfig = getClass().getResourceAsStream(configFile)) {
            ObjectMapper om = new ObjectMapper();
            return om.readValue(taskConfig, PrerenderingJobConfig.class);
        }
        catch (IOException e) {
            LOGGER.error("Could not load {}. Using empty config.", configFile, e);
            return new PrerenderingJobConfig();
        }
    }
    
    @Override
    public JobDetail createJobDetails() {
        return JobBuilder.newJob(PreRenderingJob.class)
                .withIdentity(getJobName())
                .withDescription(getJobDescription())
//                .usingJobData(REWRITE_AT_STARTUP, rewriteAtStartup)
                .build();
    }
    
    
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        if ( !enabled) {
            return;
        }
        
        LOGGER.info("Start prerendering task");
        Map<String, ConfiguredStyle> phenomenonStyles = taskConfigPrerendering.getPhenomenonStyles();
        Map<String, ConfiguredStyle> timeseriesStyles = taskConfigPrerendering.getTimeseriesStyles();
        for (String phenomenonId : phenomenonStyles.keySet()) {
            Map<String, String> parameters = new HashMap<>();
            parameters.put("phenomenon", phenomenonId);
            IoParameters query = IoParameters.createFromQuery(parameters);
            OutputCollection<TimeseriesMetadataOutput> metadatas = timeseriesMetadataService.getCondensedParameters(query);
            for (TimeseriesMetadataOutput metadata : metadatas) {
                String timeseriesId = metadata.getId();
                ConfiguredStyle style = timeseriesStyles.containsKey(timeseriesId)
                    ? timeseriesStyles.get(timeseriesId)
                    : phenomenonStyles.get(phenomenonId);
                renderConfiguredIntervals(timeseriesId, style);

                if ( !enabled) {
                    return;
                }
            }
        }

        for (String timeseriesId : timeseriesStyles.keySet()) {
            TimeseriesMetadataOutput metadata = timeseriesMetadataService.getParameter(timeseriesId);
            ParameterOutput phenomenon = metadata.getParameters().getPhenomenon();
            if (!phenomenonStyles.containsKey(phenomenon.getId())) {
                // overridden phenomena styles have been rendered already
                ConfiguredStyle style = timeseriesStyles.get(timeseriesId);
                renderConfiguredIntervals(timeseriesId, style);

                if ( !enabled) {
                    return;
                }
            }
        }
    }

    private void renderConfiguredIntervals(String timeseriesId, ConfiguredStyle style) {
        try{
            for (String interval : style.getInterval()) {
                renderWithStyle(timeseriesId, style.getStyle(), interval);
            }
        }
        catch (IOException e) {
            LOGGER.error("Error while reading prerendering configuration file!", e);
        }
    }

    private void renderWithStyle(String timeseriesId, StyleProperties style, String interval) throws IOException {
        IntervalWithTimeZone timespan = createTimespanFromInterval(timeseriesId, interval);
        IoParameters config = createConfig(timespan.toString(), style);

        TimeseriesMetadataOutput metadata = timeseriesMetadataService.getParameter(timeseriesId, config);
        RenderingContext context = createContextForSingleTimeseries(metadata, config);
        context.setDimensions(new ChartDimension(width, height));
        RequestSimpleParameterSet parameters = createForSingleTimeseries(timeseriesId, config);
        IoHandler renderer = IoFactory
                .createWith(config)
                .createIOHandler(context);
        FileOutputStream fos = createFile(timeseriesId, interval);
        renderChartFile(renderer, parameters, fos);
    }

    private void renderChartFile(IoHandler renderer, RequestSimpleParameterSet parameters, FileOutputStream fos) {
        try(FileOutputStream out = fos;) {
            renderer.generateOutput(getTimeseriesData(parameters));
            renderer.encodeAndWriteTo(out);
        }
        catch (IoParseException | IOException e) {
            LOGGER.error("Image creation occures error.", e);
        }
    }

    @Override
    public void interrupt() throws UnableToInterruptJobException {
        this.enabled = false;
        LOGGER.info("Render task successfully shutted down.");
    }

    @Override
    public void setServletConfig(ServletConfig servletConfig) {
        webappFolder = servletConfig.getServletContext().getRealPath("/");
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public ParameterService<TimeseriesMetadataOutput> getTimeseriesMetadataService() {
        return timeseriesMetadataService;
    }

    public void setTimeseriesMetadataService(ParameterService<TimeseriesMetadataOutput> timeseriesMetadataService) {
        this.timeseriesMetadataService = timeseriesMetadataService;
    }

    public SeriesDataService getTimeseriesDataService() {
        return timeseriesDataService;
    }

    public void setTimeseriesDataService(SeriesDataService timeseriesDataService) {
        this.timeseriesDataService = timeseriesDataService;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isShowGrid() {
        return showGrid;
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean hasPrerenderedImage(String timeseriesId, String interval) {
        File name = createFileName(timeseriesId, interval);
        return name.exists();
    }

    public void writePrerenderedGraphToOutputStream(String timeseriesId,
                                                    String interval,
                                                    OutputStream outputStream) {
        try {
            BufferedImage image = loadImage(timeseriesId, interval);
            if (image == null) {
                ResourceNotFoundException ex = new ResourceNotFoundException("Could not find image on server.");
                ex.addHint("Perhaps the image is being rendered at the moment. Try again later.");
                throw ex;
            }
            ImageIO.write(image, "png", outputStream);
        }
        catch (IOException e) {
            LOGGER.error("Error while loading pre rendered image", e);
        }
    }

    private BufferedImage loadImage(String timeseriesId, String interval) throws IOException {
        return ImageIO.read(new FileInputStream(createFileName(timeseriesId, interval)));
    }

    public IntervalWithTimeZone createTimespanFromInterval(String timeseriesId, String period) {
        DateTime now = new DateTime();
        if (period.equals("lastDay")) {
            Interval interval = new Interval(now.minusDays(1), now);
            return new IntervalWithTimeZone(interval.toString());
        }
        else if (period.equals("lastWeek")) {
            Interval interval = new Interval(now.minusWeeks(1), now);
            return new IntervalWithTimeZone(interval.toString());
        }
        else if (period.equals("lastMonth")) {
            Interval interval = new Interval(now.minusMonths(1), now);
            return new IntervalWithTimeZone(interval.toString());
        }
        else {
            throw new ResourceNotFoundException("Unknown interval definition '" + period + "' for timeseriesId "
                    + timeseriesId);
        }
    }

    private FileOutputStream createFile(String timeseriesId, String interval) throws IOException {
        File file = createFileName(timeseriesId, interval);
        if (file.exists()) {
            file.setLastModified(new Date().getTime());
        } else {
            file.createNewFile();
        }
        return new FileOutputStream(file);
    }

    private File createFileName(String timeseriesId, String interval) {
        String outputDirectory = getOutputFolder();
        String filename = timeseriesId + "_" + interval + ".png";
        return new File(outputDirectory + filename);
    }

    private String getOutputFolder() {
        String outputDirectory = webappFolder + File.separator + outputPath + File.separator;
        File dir = new File(outputDirectory);
        if ( !dir.exists()) {
            dir.mkdirs();
        }
        return outputDirectory;
    }

    private IoParameters createConfig(String interval, StyleProperties style) {
        Map<String, String> configuration = new HashMap<>();

        // for backward compatibility (from xml config)
        configuration.put("width", Integer.toString(width));
        configuration.put("height", Integer.toString(height));
        configuration.put("grid", Boolean.toString(showGrid));
        configuration.put("timespan", interval);
        configuration.put("locale", language);

        // overrides the above parameters (from json config)
        configuration.putAll(taskConfigPrerendering.getGeneralConfig());
        this.width = Integer.parseInt(configuration.get("width"));
        this.height = Integer.parseInt(configuration.get("height"));
        this.showGrid = Boolean.parseBoolean(configuration.get("grid"));
        this.language = configuration.get("locale");

        try {
            ObjectMapper om = new ObjectMapper();
            configuration.put("style", om.writeValueAsString(style));
        } catch (JsonProcessingException e) {
            LOGGER.warn("Invalid rendering style.", e);
        }

        return IoParameters.createFromQuery(configuration);
    }

    private TvpDataCollection getTimeseriesData(RequestSimpleParameterSet parameters) {
        return timeseriesDataService.getSeriesData(parameters);
    }

//    private final class RenderTask extends TimerTask {
//
//        @Override
//        public void run() {
//            LOGGER.info("Start prerendering task");
//            try {
//                Map<String, ConfiguredStyle> phenomenonStyles = taskConfigPrerendering.getPhenomenonStyles();
//                Map<String, ConfiguredStyle> timeseriesStyles = taskConfigPrerendering.getTimeseriesStyles();
//                for (String phenomenonId : phenomenonStyles.keySet()) {
//                    Map<String, String> parameters = new HashMap<>();
//                    parameters.put(PHENOMENON, phenomenonId);
//                    IoParameters query = IoParameters.createFromQuery(parameters);
//                    OutputCollection<SeriesMetadataV1Output> metadatas = timeseriesMetadataService.getCondensedParameters(query);
//                    for (TimeseriesMetadataOutput metadata : metadatas) {
//                        String timeseriesId = metadata.getId();
//                        ConfiguredStyle style = timeseriesStyles.containsKey(timeseriesId)
//                            ? timeseriesStyles.get(timeseriesId)
//                            : phenomenonStyles.get(phenomenonId);
//
//                        renderConfiguredIntervals(timeseriesId, style);
//                    }
//                }
//
//                for (String timeseriesId : timeseriesStyles.keySet()) {
//                    SeriesMetadataV1Output metadata = timeseriesMetadataService.getParameter(timeseriesId);
//                    PhenomenonOutput phenomenon = metadata.getParameters().getPhenomenon();
//                    if (!phenomenonStyles.containsKey(phenomenon.getId())) {
//                        // overridden phenomena styles have been rendered already
//                        ConfiguredStyle style = timeseriesStyles.get(timeseriesId);
//                        renderConfiguredIntervals(timeseriesId, style);
//                    }
//                }
//            }
//            catch (IOException e) {
//                LOGGER.error("Error while reading prerendering configuration file!", e);
//            }
//        }
//
//        private void renderConfiguredIntervals(String timeseriesId, ConfiguredStyle style) throws IOException {
//            for (String interval : style.getInterval()) {
//                renderWithStyle(timeseriesId, style.getStyle(), interval);
//            }
//        }
//
//        private void renderWithStyle(String timeseriesId, StyleProperties style, String interval) throws IOException {
//            IntervalWithTimeZone timespan = createTimespanFromInterval(timeseriesId, interval);
//            IoParameters config = createConfig(timespan.toString(), style);
//
//            TimeseriesMetadataOutput metadata = timeseriesMetadataService.getParameter(timeseriesId, config);
//            RenderingContext context = createContextForSingleTimeseries(metadata, config);
//            context.setDimensions(new ChartDimension(width, height));
//            RequestSimpleParameterSet parameters = createForSingleTimeseries(timeseriesId, config);
//            IoHandler renderer = IoFactory
//                    .createWith(config)
//                    .createIOHandler(context);
//            FileOutputStream fos = createFile(timeseriesId, interval);
//            renderChartFile(renderer, parameters, fos);
//        }
//
//        private void renderChartFile(IoHandler renderer, RequestSimpleParameterSet parameters, FileOutputStream fos) {
//            try {
//                renderer.generateOutput(getTimeseriesData(parameters));
//                renderer.encodeAndWriteTo(fos);
//            }
//            catch (IoParseException e) {
//                LOGGER.error("Image creation occures error.", e);
//            }
//            finally {
//                try {
//                    fos.flush();
//                    fos.close();
//                }
//                catch (IOException e) {
//                    LOGGER.error("File stream already flushed/closed.", e);
//                }
//            }
//        }
//    }
}
