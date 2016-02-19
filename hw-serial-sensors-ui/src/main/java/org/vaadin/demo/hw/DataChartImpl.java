/*
 * Copyright 2000-2014 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.vaadin.demo.hw;

import com.vaadin.addon.charts.Chart;
import com.vaadin.addon.charts.model.ChartType;
import com.vaadin.addon.charts.model.Configuration;
import com.vaadin.addon.charts.model.HorizontalAlign;
import com.vaadin.addon.charts.model.LayoutDirection;
import com.vaadin.addon.charts.model.Legend;
import com.vaadin.addon.charts.model.ListSeries;
import com.vaadin.addon.charts.model.PlotOptionsLine;
import com.vaadin.addon.charts.model.Series;
import com.vaadin.addon.charts.model.VerticalAlign;
import com.vaadin.addon.charts.model.YAxis;

import java.util.List;
import java.util.StringTokenizer;

/**
 * Provides various helper methods for connectors. Meant for internal use.
 *
 * @author Vaadin Ltd
 */
public class DataChartImpl implements SerialSensorUI.DataChart {

    public static final int MAX_POINTS = 50;

    private final Chart chart;
    private final List<Series> series;
    private Number[][] data = new Number[5][];


    public DataChartImpl() {
        for (int i = 0; i < data.length; i++) {
            data[i] = new Number[MAX_POINTS];

        }
        chart = new Chart();

        Configuration configuration = chart.getConfiguration();
        configuration.getChart().setType(ChartType.LINE);
        configuration.getChart().setMarginRight(200);
        configuration.getChart().setMarginBottom(25);
        configuration.getTitle().setText("Vaadin Serial Sensors Demo");
        configuration.getSubTitle().setText("STM X-Nucleo IKS01A1");
        configuration.getxAxis().setMin(0);
        configuration.getxAxis().setMax(MAX_POINTS - 1);
        configuration.getChart().setAnimation(false);
        YAxis primaryAxis = configuration.getyAxis();
        primaryAxis.setMin(-20);
        primaryAxis.setMax(100);

        YAxis secondaryAxis = new YAxis();
        secondaryAxis.setMax(1200);
        secondaryAxis.setMin(-1200);
        secondaryAxis.setOpposite(true);

        configuration.addyAxis(secondaryAxis);

        createSeries(configuration, primaryAxis, "T, C");
        createSeries(configuration, primaryAxis, "Humidity, %");
        createSeries(configuration, secondaryAxis, "Acc X, mG");
        createSeries(configuration, secondaryAxis, "Acc Y, mG");
        createSeries(configuration, secondaryAxis, "Acc Z, mG");

        Legend legend = configuration.getLegend();
        legend.setLayout(LayoutDirection.VERTICAL);
        legend.setAlign(HorizontalAlign.RIGHT);
        legend.setVerticalAlign(VerticalAlign.TOP);
        legend.setX(-10d);
        legend.setY(100d);
        legend.setBorderWidth(0);

        series = configuration.getSeries();

    }

    private void createSeries(Configuration configuration, YAxis axis, String name) {

        ListSeries aSeries = new ListSeries(name);
        PlotOptionsLine plotOptions = new PlotOptionsLine();
        plotOptions.setAnimation(false);
        aSeries.setPlotOptions(plotOptions);
        configuration.addSeries(aSeries);
        aSeries.setyAxis(axis);
    }

    @Override
    public Chart getComponent() {
        return chart;
    }

    /*
    Strings to be parsed:
                TimeStamp: 00:00:02.29
                MAG_X[0]: 383, MAG_Y[0]: -327, MAG_Z[0]: 156
                HUM[0]: 46.59
                TEMP[0]: 24.60
    */
    @Override
    public void processLine(String line) {
        if (line == null) return;
        if (line.startsWith("TimeStamp:")) {
            stepChart();
        } else if (!updateSingle(line, "HUM[0]:", 1)) {
            if (!updateSingle(line, "TEMP[0]:", 0)) {
                updateAcceleration(line);
            }
        }
    }

    private void updateAcceleration(String line) {
        if (!line.startsWith("ACC_X[0]:")) return;
        StringTokenizer tokenizer = new StringTokenizer(line, ":,");
        for (int i = 2; i < 5; i++) {
            tokenizer.nextToken();
            String s = tokenizer.nextToken().trim();
            data[i][0] = Double.parseDouble(s);
        }
    }

    private boolean updateSingle(String line, String prefix, int index) {
        if (!line.startsWith(prefix)) return false;
        String s = line.substring(prefix.length()).trim();
        double v = Double.parseDouble(s);
        data[index][0] = v;
        return true;
    }


    private void stepChart() {
        for (int i = 0; i < data.length; i++) {
            System.arraycopy(data[i], 0, data[i], 1, MAX_POINTS - 1);
            ListSeries listSeries = (ListSeries) series.get(i);
            listSeries.setData(data[i]);
        }
        chart.drawChart();
    }
}
