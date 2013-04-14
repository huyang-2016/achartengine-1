/**
 * Copyright (C) 2013 Henning Dodenhof
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.achartengine;

import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.tools.MoveListener;
import org.achartengine.tools.PanListener;
import org.achartengine.tools.ZoomEvent;
import org.achartengine.tools.ZoomListener;

import android.content.Context;
import android.graphics.Color;
import android.view.View;

public class ConnectedCharts {

  private static final String TAG = ConnectedCharts.class.getSimpleName();

  private static final int OVERLAY_COLOR = Color.argb(128, 0, 0, 0); // TODO

  private Context mContext;

  private GraphicalView mBaseChartView;
  private GraphicalView mOverviewChartView;

  private XYMultipleSeriesRenderer mBaseChartRenderer;
  private XYMultipleSeriesRenderer mOverviewChartRenderer;

  private XYMultipleSeriesDataset mBaseDataset;
  private XYMultipleSeriesDataset mOverviewDataset;

  private String[] mTypes;
  private int mOverlayIndex;

  public ConnectedCharts(Context context, XYMultipleSeriesDataset dataset,
      XYMultipleSeriesRenderer renderer, String[] types) {
    mContext = context;

    mBaseChartView = ChartFactory.getCombinedXYChartView(context, dataset, renderer, types);
    mBaseDataset = dataset;
    mBaseChartRenderer = renderer;
    mTypes = types;

    mBaseChartRenderer.setPanEnabled(true, false);
    mBaseChartRenderer.setZoomEnabled(true, false);
    mBaseChartRenderer.setZoomButtonsVisible(false);

    initOverviewChart();
    initListeners();
  }

  private void initOverviewChart() {
    mOverviewDataset = new XYMultipleSeriesDataset();
    mOverviewChartRenderer = new XYMultipleSeriesRenderer();

    // Keep track of base series min/max values
    double minX = Double.MAX_VALUE;
    double maxX = Double.MIN_VALUE;
    double minY = Double.MAX_VALUE;
    double maxY = Double.MIN_VALUE;

    // Copy base series to overview
    for (int i = 0; i < mBaseDataset.getSeriesCount(); i++) {
      XYSeries baseSeries = mBaseDataset.getSeriesAt(i);
      XYSeriesRenderer baseSeriesRenderer = (XYSeriesRenderer) mBaseChartRenderer
          .getSeriesRendererAt(i);

      XYSeries series = new XYSeries(baseSeries.getTitle());
      mOverviewDataset.addSeries(series);

      XYSeriesRenderer overviewSeriesRenderer = new XYSeriesRenderer();
      mOverviewChartRenderer.addSeriesRenderer(overviewSeriesRenderer);

      // TODO
      overviewSeriesRenderer.setColor(baseSeriesRenderer.getColor());
      overviewSeriesRenderer.setPointStrokeWidth(2);
      overviewSeriesRenderer.setLineWidth(2);

      for (int j = 0; j < baseSeries.getItemCount(); j++) {
        series.add(baseSeries.getX(j), baseSeries.getY(j));
      }

      minX = Math.min(minX, baseSeries.getMinX());
      maxX = Math.max(maxX, baseSeries.getMaxX());
      minY = Math.min(minY, baseSeries.getMinY());
      maxY = Math.max(maxY, baseSeries.getMaxY());
    }

    // Add overlay series to overview
    XYSeries overlaySeries = new XYSeries("Overlay");
    mOverviewDataset.addSeries(overlaySeries);

    XYSeriesRenderer overlayRenderer = new XYSeriesRenderer();
    mOverviewChartRenderer.addSeriesRenderer(overlayRenderer);

    overlayRenderer.setColor(OVERLAY_COLOR);

    String[] overlayTypes = new String[mTypes.length + 1];
    for (int i = 0; i < mTypes.length; i++) {
      overlayTypes[i] = mTypes[i];
    }
    overlayTypes[mTypes.length] = "Overlay";
    mOverlayIndex = mTypes.length;

    // Setup initial overlay
    double center = (maxX - minX) / 2 + minX;
    double margin = (maxX - minX) / 4; // TODO

    overlaySeries.add(center - margin, 0);
    overlaySeries.add(center + margin, 0);

    // Setup limits for both charts based on base series min/max values
    mOverviewChartRenderer.setXAxisMin(minX - 1);
    mOverviewChartRenderer.setXAxisMax(maxX + 1);
    mOverviewChartRenderer.setYAxisMin(minY - 1);
    mOverviewChartRenderer.setYAxisMax(maxY + 1);
    mOverviewChartRenderer.setPanLimits(new double[] { minX - 1, maxX + 1, minY - 1, maxY + 1 });

    // this is initOverviewChart(), should go somewhere else
    mBaseChartRenderer.setXAxisMin(center - margin);
    mBaseChartRenderer.setXAxisMax(center + margin);
    mBaseChartRenderer.setYAxisMin(minY - 1);
    mBaseChartRenderer.setYAxisMax(maxY + 1);
    mBaseChartRenderer.setZoomLimits(new double[] { minX - 1, maxX + 1, minY - 1, maxY + 1 });
    mBaseChartRenderer.setPanLimits(new double[] { minX - 1, maxX + 1, minY - 1, maxY + 1 });

    // Disable panning and zooming
    mOverviewChartRenderer.setPanEnabled(false);
    mOverviewChartRenderer.setZoomEnabled(false);
    mOverviewChartRenderer.setZoomButtonsVisible(false);

    // create overview chart and view
    mOverviewChartView = ChartFactory.getCombinedXYChartView(mContext, mOverviewDataset,
        mOverviewChartRenderer, overlayTypes);
  }

  private void initListeners() {
    mBaseChartView.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        // TODO Touch doesn't work without this listener - why?
      }
    });

    mBaseChartView.addPanListener(new PanListener() {
      @Override
      public void panApplied() {
        updateOverview();
      }
    });

    mBaseChartView.addZoomListener(new ZoomListener() {
      @Override
      public void zoomReset() {
        updateOverview();
      }

      @Override
      public void zoomApplied(ZoomEvent arg0) {
        updateOverview();
      }
    }, true, true);

    mOverviewChartView.addMoveListener(new MoveListener() {
      @Override
      public void moveApplied() {
        mOverviewChartView.repaint();

        mBaseChartRenderer.setXAxisMin(mOverviewDataset.getSeriesAt(mOverlayIndex).getX(0));
        mBaseChartRenderer.setXAxisMax(mOverviewDataset.getSeriesAt(mOverlayIndex).getX(1));
        mBaseChartView.repaint();
      }
    });
  }

  private void updateOverview() {
    XYSeries overlaySeries = mOverviewDataset.getSeriesAt(mOverlayIndex);

    for (int i = overlaySeries.getItemCount() - 1; i >= 0; i--) {
      overlaySeries.remove(i);
    }

    overlaySeries.add(mBaseChartRenderer.getXAxisMin(), 0);
    overlaySeries.add(mBaseChartRenderer.getXAxisMax(), 0);

    mOverviewChartView.repaint();
  }

  public void repaint() {
    mBaseChartView.repaint();
    mOverviewChartView.repaint();
  }

  // TODO this enables messing around with charts and renderers and possibly
  // breaking everything; we need a more robust way to allow safe customization

  public GraphicalView getBaseChartView() {
    return mBaseChartView;
  }

  public GraphicalView getOverviewChartView() {
    return mOverviewChartView;
  }

  public XYMultipleSeriesRenderer getBaseChartRenderer() {
    return mBaseChartRenderer;
  }

  public XYMultipleSeriesRenderer getOverviewChartRenderer() {
    return mOverviewChartRenderer;
  }
}
