// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.googlemaps;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static io.flutter.plugins.googlemaps.Convert.HEATMAP_DATA_KEY;
import static io.flutter.plugins.googlemaps.Convert.HEATMAP_GRADIENT_COLORS_KEY;
import static io.flutter.plugins.googlemaps.Convert.HEATMAP_GRADIENT_COLOR_MAP_SIZE_KEY;
import static io.flutter.plugins.googlemaps.Convert.HEATMAP_GRADIENT_KEY;
import static io.flutter.plugins.googlemaps.Convert.HEATMAP_GRADIENT_START_POINTS_KEY;
import static io.flutter.plugins.googlemaps.Convert.HEATMAP_ID_KEY;
import static io.flutter.plugins.googlemaps.Convert.HEATMAP_MAX_INTENSITY_KEY;
import static io.flutter.plugins.googlemaps.Convert.HEATMAP_OPACITY_KEY;
import static io.flutter.plugins.googlemaps.Convert.HEATMAP_RADIUS_KEY;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.algo.StaticCluster;
import com.google.maps.android.geometry.Point;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;
import com.google.maps.android.projection.SphericalMercatorProjection;

import io.flutter.plugins.googlemaps.Convert.BitmapDescriptorFactoryWrapper;
import io.flutter.plugins.googlemaps.Convert.FlutterInjectorWrapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(minSdk = Build.VERSION_CODES.P)
public class ConvertTest {
  @Mock private AssetManager assetManager;

  @Mock private BitmapDescriptorFactoryWrapper bitmapDescriptorFactoryWrapper;

  @Mock private BitmapDescriptor mockBitmapDescriptor;

  @Mock private FlutterInjectorWrapper flutterInjectorWrapper;

  AutoCloseable mockCloseable;

  // A 1x1 pixel (#8080ff) PNG image encoded in base64
  private String base64Image = generateBase64Image();

  @Before
  public void before() {
    mockCloseable = MockitoAnnotations.openMocks(this);
  }

  @After
  public void tearDown() throws Exception {
    mockCloseable.close();
  }

  @Test
  public void ConvertToPointsConvertsThePointsWithFullPrecision() {
    double latitude = 43.03725568057;
    double longitude = -87.90466904649;
    ArrayList<Double> point = new ArrayList<Double>();
    point.add(latitude);
    point.add(longitude);
    ArrayList<ArrayList<Double>> pointsList = new ArrayList<>();
    pointsList.add(point);
    List<LatLng> latLngs = Convert.toPoints(pointsList);
    LatLng latLng = latLngs.get(0);
    Assert.assertEquals(latitude, latLng.latitude, 1e-15);
    Assert.assertEquals(longitude, latLng.longitude, 1e-15);
  }

  @Test
  public void ConvertClusterToPigeonReturnsCorrectData() {
    String clusterManagerId = "cm_1";
    LatLng clusterPosition = new LatLng(43.00, -87.90);
    LatLng markerPosition1 = new LatLng(43.05, -87.95);
    LatLng markerPosition2 = new LatLng(43.02, -87.92);

    StaticCluster<MarkerBuilder> cluster = new StaticCluster<>(clusterPosition);

    MarkerBuilder marker1 = new MarkerBuilder("m_1", clusterManagerId);
    marker1.setPosition(markerPosition1);
    cluster.add(marker1);

    MarkerBuilder marker2 = new MarkerBuilder("m_2", clusterManagerId);
    marker2.setPosition(markerPosition2);
    cluster.add(marker2);

    Messages.PlatformCluster result = Convert.clusterToPigeon(clusterManagerId, cluster);
    Assert.assertEquals(clusterManagerId, result.getClusterManagerId());

    Messages.PlatformLatLng position = result.getPosition();
    Assert.assertEquals(clusterPosition.latitude, position.getLatitude(), 1e-15);
    Assert.assertEquals(clusterPosition.longitude, position.getLongitude(), 1e-15);

    Messages.PlatformLatLngBounds bounds = result.getBounds();
    Messages.PlatformLatLng southwest = bounds.getSouthwest();
    Messages.PlatformLatLng northeast = bounds.getNortheast();
    // bounding data should combine data from marker positions markerPosition1 and markerPosition2
    Assert.assertEquals(markerPosition2.latitude, southwest.getLatitude(), 1e-15);
    Assert.assertEquals(markerPosition1.longitude, southwest.getLongitude(), 1e-15);
    Assert.assertEquals(markerPosition1.latitude, northeast.getLatitude(), 1e-15);
    Assert.assertEquals(markerPosition2.longitude, northeast.getLongitude(), 1e-15);

    List<String> markerIds = result.getMarkerIds();
    Assert.assertEquals(2, markerIds.size());
    Assert.assertEquals(marker1.markerId(), markerIds.get(0));
    Assert.assertEquals(marker2.markerId(), markerIds.get(1));
  }

  @Test
  public void GetBitmapFromAssetAuto() throws Exception {
    String fakeAssetName = "fake_asset_name";
    String fakeAssetKey = "fake_asset_key";
    Map<String, Object> assetDetails = new HashMap<>();
    assetDetails.put("assetName", fakeAssetName);
    assetDetails.put("bitmapScaling", "auto");
    assetDetails.put("width", 15.0f);
    assetDetails.put("height", 15.0f);
    assetDetails.put("imagePixelRatio", 2.0f);

    when(flutterInjectorWrapper.getLookupKeyForAsset(fakeAssetName)).thenReturn(fakeAssetKey);

    when(assetManager.open(fakeAssetKey)).thenReturn(buildImageInputStream());

    when(bitmapDescriptorFactoryWrapper.fromBitmap(any())).thenReturn(mockBitmapDescriptor);

    BitmapDescriptor result =
        Convert.getBitmapFromAsset(
            assetDetails,
            assetManager,
            1.0f,
            bitmapDescriptorFactoryWrapper,
            flutterInjectorWrapper);

    Assert.assertEquals(mockBitmapDescriptor, result);
  }

  @Test
  public void GetBitmapFromAssetAutoAndWidth() throws Exception {
    String fakeAssetName = "fake_asset_name";
    String fakeAssetKey = "fake_asset_key";

    Map<String, Object> assetDetails = new HashMap<>();
    assetDetails.put("assetName", fakeAssetName);
    assetDetails.put("bitmapScaling", "auto");
    assetDetails.put("width", 15.0f);
    assetDetails.put("imagePixelRatio", 2.0f);

    when(flutterInjectorWrapper.getLookupKeyForAsset(fakeAssetName)).thenReturn(fakeAssetKey);

    when(assetManager.open(fakeAssetKey)).thenReturn(buildImageInputStream());

    when(bitmapDescriptorFactoryWrapper.fromBitmap(any())).thenReturn(mockBitmapDescriptor);

    BitmapDescriptor result =
        Convert.getBitmapFromAsset(
            assetDetails,
            assetManager,
            1.0f,
            bitmapDescriptorFactoryWrapper,
            flutterInjectorWrapper);

    Assert.assertEquals(mockBitmapDescriptor, result);
  }

  @Test
  public void GetBitmapFromAssetAutoAndHeight() throws Exception {
    String fakeAssetName = "fake_asset_name";
    String fakeAssetKey = "fake_asset_key";

    Map<String, Object> assetDetails = new HashMap<>();
    assetDetails.put("assetName", fakeAssetName);
    assetDetails.put("bitmapScaling", "auto");
    assetDetails.put("height", 15.0f);
    assetDetails.put("imagePixelRatio", 2.0f);

    when(flutterInjectorWrapper.getLookupKeyForAsset(fakeAssetName)).thenReturn(fakeAssetKey);

    when(assetManager.open(fakeAssetKey)).thenReturn(buildImageInputStream());

    when(bitmapDescriptorFactoryWrapper.fromBitmap(any())).thenReturn(mockBitmapDescriptor);

    BitmapDescriptor result =
        Convert.getBitmapFromAsset(
            assetDetails,
            assetManager,
            1.0f,
            bitmapDescriptorFactoryWrapper,
            flutterInjectorWrapper);

    Assert.assertEquals(mockBitmapDescriptor, result);
  }

  @Test
  public void GetBitmapFromAssetNoScaling() throws Exception {
    String fakeAssetName = "fake_asset_name";
    String fakeAssetKey = "fake_asset_key";

    Map<String, Object> assetDetails = new HashMap<>();
    assetDetails.put("assetName", fakeAssetName);
    assetDetails.put("bitmapScaling", "noScaling");
    assetDetails.put("imagePixelRatio", 2.0f);

    when(flutterInjectorWrapper.getLookupKeyForAsset(fakeAssetName)).thenReturn(fakeAssetKey);

    when(assetManager.open(fakeAssetKey)).thenReturn(buildImageInputStream());

    when(bitmapDescriptorFactoryWrapper.fromAsset(any())).thenReturn(mockBitmapDescriptor);

    verify(bitmapDescriptorFactoryWrapper, never()).fromBitmap(any());

    BitmapDescriptor result =
        Convert.getBitmapFromAsset(
            assetDetails,
            assetManager,
            1.0f,
            bitmapDescriptorFactoryWrapper,
            flutterInjectorWrapper);

    Assert.assertEquals(mockBitmapDescriptor, result);
  }

  @Test
  public void GetBitmapFromBytesAuto() throws Exception {
    byte[] bmpData = Base64.decode(base64Image, Base64.DEFAULT);

    Map<String, Object> assetDetails = new HashMap<>();
    assetDetails.put("byteData", bmpData);
    assetDetails.put("bitmapScaling", "auto");
    assetDetails.put("imagePixelRatio", 2.0f);

    when(bitmapDescriptorFactoryWrapper.fromBitmap(any())).thenReturn(mockBitmapDescriptor);

    BitmapDescriptor result =
        Convert.getBitmapFromBytes(assetDetails, 1f, bitmapDescriptorFactoryWrapper);

    Assert.assertEquals(mockBitmapDescriptor, result);
  }

  @Test
  public void GetBitmapFromBytesAutoAndWidth() throws Exception {
    byte[] bmpData = Base64.decode(base64Image, Base64.DEFAULT);

    Map<String, Object> assetDetails = new HashMap<>();
    assetDetails.put("byteData", bmpData);
    assetDetails.put("bitmapScaling", "auto");
    assetDetails.put("imagePixelRatio", 2.0f);
    assetDetails.put("width", 15.0f);

    when(bitmapDescriptorFactoryWrapper.fromBitmap(any())).thenReturn(mockBitmapDescriptor);

    BitmapDescriptor result =
        Convert.getBitmapFromBytes(assetDetails, 1f, bitmapDescriptorFactoryWrapper);

    Assert.assertEquals(mockBitmapDescriptor, result);
  }

  @Test
  public void GetBitmapFromBytesAutoAndHeight() throws Exception {
    byte[] bmpData = Base64.decode(base64Image, Base64.DEFAULT);

    Map<String, Object> assetDetails = new HashMap<>();
    assetDetails.put("byteData", bmpData);
    assetDetails.put("bitmapScaling", "auto");
    assetDetails.put("imagePixelRatio", 2.0f);
    assetDetails.put("height", 15.0f);

    when(bitmapDescriptorFactoryWrapper.fromBitmap(any())).thenReturn(mockBitmapDescriptor);

    BitmapDescriptor result =
        Convert.getBitmapFromBytes(assetDetails, 1f, bitmapDescriptorFactoryWrapper);

    Assert.assertEquals(mockBitmapDescriptor, result);
  }

  @Test
  public void GetBitmapFromBytesNoScaling() throws Exception {
    byte[] bmpData = Base64.decode(base64Image, Base64.DEFAULT);

    Map<String, Object> assetDetails = new HashMap<>();
    assetDetails.put("byteData", bmpData);
    assetDetails.put("bitmapScaling", "noScaling");
    assetDetails.put("imagePixelRatio", 2.0f);

    when(bitmapDescriptorFactoryWrapper.fromBitmap(any())).thenReturn(mockBitmapDescriptor);

    BitmapDescriptor result =
        Convert.getBitmapFromBytes(assetDetails, 1f, bitmapDescriptorFactoryWrapper);

    Assert.assertEquals(mockBitmapDescriptor, result);
  }

  @Test(expected = IllegalArgumentException.class) // Expecting an IllegalArgumentException
  public void GetBitmapFromBytesThrowsErrorIfInvalidImageData() throws Exception {
    String invalidBase64Image = "not valid image data";
    byte[] bmpData = Base64.decode(invalidBase64Image, Base64.DEFAULT);

    Map<String, Object> assetDetails = new HashMap<>();
    assetDetails.put("byteData", bmpData);
    assetDetails.put("bitmapScaling", "noScaling");
    assetDetails.put("imagePixelRatio", 2.0f);

    verify(bitmapDescriptorFactoryWrapper, never()).fromBitmap(any());

    try {
      Convert.getBitmapFromBytes(assetDetails, 1f, bitmapDescriptorFactoryWrapper);
    } catch (IllegalArgumentException e) {
      Assert.assertEquals(e.getMessage(), "Unable to interpret bytes as a valid image.");
      throw e; // rethrow the exception
    }

    fail("Expected an IllegalArgumentException to be thrown");
  }

  private static final SphericalMercatorProjection sProjection =
          new SphericalMercatorProjection(1);

  @Test()
  public void ConvertToWeightedLatLngReturnsCorrectData() {
    final Object data = List.of(List.of(1.1, 2.2), 3.3);
    final Point point = sProjection.toPoint(new LatLng(1.1, 2.2));

    final WeightedLatLng result = Convert.toWeightedLatLng(data);

    Assert.assertEquals(point.x, result.getPoint().x, 0);
    Assert.assertEquals(point.y, result.getPoint().y, 0);
    Assert.assertEquals(3.3, result.getIntensity(), 0);
  }

  @Test()
  public void ConvertToWeightedDataReturnsCorrectData() {
    final List<Object> data = List.of(List.of(List.of(1.1, 2.2), 3.3));
    final Point point = sProjection.toPoint(new LatLng(1.1, 2.2));

    final List<WeightedLatLng> result = Convert.toWeightedData(data);

    Assert.assertEquals(1, result.size());
    Assert.assertEquals(point.x, result.get(0).getPoint().x, 0);
    Assert.assertEquals(point.y, result.get(0).getPoint().y, 0);
    Assert.assertEquals(3.3, result.get(0).getIntensity(), 0);
  }

  @Test()
  public void ConvertToGradientReturnsCorrectData() {
    final List<Object> colorData = List.of(0, 1, 2);
    List<Object> startPointData = List.of(0.0, 1.0, 2.0);
    final Map<String, Object> data = Map.of(
            HEATMAP_GRADIENT_COLORS_KEY, colorData,
            HEATMAP_GRADIENT_START_POINTS_KEY, startPointData,
            HEATMAP_GRADIENT_COLOR_MAP_SIZE_KEY, 3
    );

    final Gradient result = Convert.toGradient(data);

    Assert.assertEquals(3, result.mColors.length);
    Assert.assertEquals(0, result.mColors[0]);
    Assert.assertEquals(1, result.mColors[1]);
    Assert.assertEquals(2, result.mColors[2]);
    Assert.assertEquals(3, result.mStartPoints.length);
    Assert.assertEquals(0.0, result.mStartPoints[0], 0);
    Assert.assertEquals(1.0, result.mStartPoints[1], 0);
    Assert.assertEquals(2.0, result.mStartPoints[2], 0);
    Assert.assertEquals(3, result.mColorMapSize);
  }

  @Test()
  public void ConvertInterpretHeatmapOptionsReturnsCorrectData() {
    final List<Object> dataData = List.of(List.of(List.of(1.1, 2.2), 3.3));
    final Point point = sProjection.toPoint(new LatLng(1.1, 2.2));
    final Map<String, Object> data = Map.of(
            HEATMAP_DATA_KEY, dataData,
            HEATMAP_GRADIENT_KEY, Map.of(
                    HEATMAP_GRADIENT_COLORS_KEY, List.of(0, 1, 2),
                    HEATMAP_GRADIENT_START_POINTS_KEY, List.of(0.0, 1.0, 2.0),
                    HEATMAP_GRADIENT_COLOR_MAP_SIZE_KEY, 3
            ),
            HEATMAP_MAX_INTENSITY_KEY, 4.4,
            HEATMAP_OPACITY_KEY, 5.5,
            HEATMAP_RADIUS_KEY, 6,
            HEATMAP_ID_KEY, "heatmap_1"
    );

    final MockHeatmapBuilder builder = new MockHeatmapBuilder();
    final String id = Convert.interpretHeatmapOptions(data, builder);

    Assert.assertEquals(1, builder.getWeightedData().size());
    Assert.assertEquals(point.x, builder.getWeightedData().get(0).getPoint().x, 0);
    Assert.assertEquals(point.y, builder.getWeightedData().get(0).getPoint().y, 0);
    Assert.assertEquals(3.3, builder.getWeightedData().get(0).getIntensity(), 0);
    Assert.assertEquals(3, builder.getGradient().mColors.length);
    Assert.assertEquals(0, builder.getGradient().mColors[0]);
    Assert.assertEquals(1, builder.getGradient().mColors[1]);
    Assert.assertEquals(2, builder.getGradient().mColors[2]);
    Assert.assertEquals(3, builder.getGradient().mStartPoints.length);
    Assert.assertEquals(0.0, builder.getGradient().mStartPoints[0], 0);
    Assert.assertEquals(1.0, builder.getGradient().mStartPoints[1], 0);
    Assert.assertEquals(2.0, builder.getGradient().mStartPoints[2], 0);
    Assert.assertEquals(3, builder.getGradient().mColorMapSize);
    Assert.assertEquals(4.4, builder.getMaxIntensity(), 0);
    Assert.assertEquals(5.5, builder.getOpacity(), 0);
    Assert.assertEquals(6, builder.getRadius());
    Assert.assertEquals("heatmap_1", id);
  }

  private InputStream buildImageInputStream() {
    Bitmap fakeBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    fakeBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
    byte[] byteArray = byteArrayOutputStream.toByteArray();
    InputStream fakeStream = new ByteArrayInputStream(byteArray);
    return fakeStream;
  }

  // Helper method to generate 1x1 pixel base64 encoded png test image
  private String generateBase64Image() {
    int width = 1;
    int height = 1;
    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);

    // Draw on the Bitmap
    Paint paint = new Paint();
    paint.setColor(Color.parseColor("#FF8080FF"));
    canvas.drawRect(0, 0, width, height, paint);

    // Convert the Bitmap to PNG format
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
    byte[] pngBytes = outputStream.toByteArray();

    // Encode the PNG bytes as a base64 string
    String base64Image = Base64.encodeToString(pngBytes, Base64.DEFAULT);

    return base64Image;
  }
}

class MockHeatmapBuilder implements HeatmapOptionsSink {
  private List<WeightedLatLng> weightedData;
    private Gradient gradient;
    private double maxIntensity;
    private double opacity;
    private int radius;

    public List<WeightedLatLng> getWeightedData() {
      return weightedData;
    }

    public Gradient getGradient() {
      return gradient;
    }

    public double getMaxIntensity() {
      return maxIntensity;
    }

    public double getOpacity() {
      return opacity;
    }

    public int getRadius() {
      return radius;
    }

  @Override
  public void setWeightedData(@NonNull List<WeightedLatLng> weightedData) {
    this.weightedData = weightedData;
  }

  @Override
  public void setGradient(Gradient gradient) {
    this.gradient = gradient;
  }

  @Override
  public void setMaxIntensity(double maxIntensity) {
    this.maxIntensity = maxIntensity;
  }

  @Override
  public void setOpacity(double opacity) {
    this.opacity = opacity;
  }

  @Override
  public void setRadius(int radius) {
    this.radius = radius;
  }
}