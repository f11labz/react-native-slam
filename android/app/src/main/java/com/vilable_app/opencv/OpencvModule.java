package com.vilable_app.opencv;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.vilable_app.ocr.OcrModule;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONObject;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpencvModule extends ReactContextBaseJavaModule {
    private static ReactApplicationContext reactContext;
    OpencvModule(ReactApplicationContext context) {
        super(context);
        this.reactContext = context;
        Log.d("constructor", "opencv initializing");
        OpenCVLoader.initDebug();
        Log.d("constructor", "opencv initializing success");
    }

    @Override
    public String getName() {
        return "VilaOpencvModule";
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        return constants;
    }

    @ReactMethod
    public void detectPoints(String path, final Callback callback){

        byte[] decodedString = Base64.decode(path, Base64.DEFAULT);
        Bitmap image = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        try{
//            Log.d("Opencv","getting point");
            List<Point> points = imageContour(image);
//            WritableArray data = Arguments.fromList(points);
//            Log.d("Opencv","points detected");
            WritableArray array = new WritableNativeArray();
            for (Point p : points) {
                WritableArray a = new WritableNativeArray();
                a.pushDouble(p.x);
                a.pushDouble(p.y);
                array.pushArray(a);
            }
//            Log.d("Opencv","returning point");
            callback.invoke(null, array);
        }
        catch (Exception  e) {
            System.out.print("Exception:" + e.toString());
            callback.invoke(e.toString(), null);
        }
    }

    public List<Point> imageContour(Bitmap image) {
//        Log.d("imageContour", "imageContour started");
        Mat orignalImage = new Mat();
        Bitmap bmp32 = image.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, orignalImage);
//        final Mat orignalImage = Imgcodecs.imread(f.getAbsolutePath());
//        Mat finalMat = new Mat(orignalImage.rows(), orignalImage.cols(), orignalImage.type());
        Mat tmpImage = new Mat();
        Mat detectedEdges = new Mat();
        final Mat hierarchy = new Mat();
        final List<MatOfPoint> contours = new ArrayList<>();
//        Scalar red = new Scalar(255, 0, 0);
//        Scalar blue = new Scalar(0, 0, 255);
//        Scalar green = new Scalar(0, 255, 0);
//        Scalar white = new Scalar(255, 255, 255);
//        Scalar yellow = new Scalar(255, 222, 0);
//        Scalar black = new Scalar(0, 0, 0);

        // convert to grey scale
        Imgproc.cvtColor(orignalImage, tmpImage, Imgproc.COLOR_RGBA2GRAY);

        // blur image & show
        Imgproc.blur(tmpImage, tmpImage, new Size(3, 3));
        //BlurImageFragment.getInstance().showMatImage(tmpImage);

        // use threshold as set by preferences
        double maxThreshold = 2.5 * 1.0;
        double minThreshold = 7.5 * 1.0;
        // if ostu threshold is enabled, then use it instead
        if (true) {
            Mat tmp2 = new Mat();
            double otsuThreshold = Imgproc.threshold(tmpImage, tmp2, (double) 0, (double) 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
            maxThreshold = otsuThreshold;
            minThreshold = otsuThreshold * 0.5;
            tmp2.release();
        }
        // detect & show edges
        Imgproc.Canny(tmpImage, detectedEdges, minThreshold, maxThreshold);
        //CannyEdgesFragment.getInstance().showMatImage(detectedEdges);
//        Log.d("imageContour", "findContour started");
        // find contours
        Imgproc.findContours(detectedEdges, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE);
//        Log.d("imageContour", "findContour ended");
        hierarchy.release();
        // iterate over all identified contours and analyze them
        MatOfPoint2f contour2f;
        List<Point> pointList = new ArrayList<Point>();
        for (int i = 0; i < contours.size(); i++) {
            // do some approximations to the idenitfied curves
            contour2f = new MatOfPoint2f(contours.get(i).toArray());
            double approxDistance = 0.03 * Imgproc.arcLength(contour2f, true);
            MatOfPoint2f approxCurve2f = new MatOfPoint2f();
            Imgproc.approxPolyDP(contour2f, approxCurve2f, approxDistance, true);
            MatOfPoint approxCurve = new MatOfPoint();
            approxCurve2f.convertTo(approxCurve, CvType.CV_32S);
            Point[] p = approxCurve2f.toArray();
            List points = Arrays.asList(p);
            pointList.addAll(points);
//            for (int j = 0; j < p.length; j++) {
//                //Log.e(TAG, "con " +  i + "point: "+j+ "x: " +p[j].x + "y: " +p[j].y);
//            }


            // filter based on number of vertices
            // Log.d(TAG, "number of points = " + approxCurve.total());
//            int vertices = approxCurve.height();
////            Log.d(TAG, "Vertices = " + vertices);
//            if (vertices > 3 && vertices <9) {
//                //Log.e(TAG, "Found rectangle");
//                //Imgproc.drawContours(detectedEdges, contours, i, white, -1);
//                Imgproc.drawContours(finalMat, contours, i, red, 0, 8, hierarchy, 0, new Point(0, 0));
//
//            } else {
//
//                //Log.e(TAG, "skipping vertices = " + vertices);
//            }
        }
        orignalImage.release();
        detectedEdges.release();
        tmpImage.release();
        return pointList;
    }

    public class Quadrilateral {
        public MatOfPoint contour;
        public Point[] points;

        public Quadrilateral(MatOfPoint contour, Point[] points) {
            this.contour = contour;
            this.points = points;
        }
    }

    @ReactMethod
    public void findContourPolygon( String path, Double area, final Callback callback ) {
        byte[] decodedString = Base64.decode(path, Base64.DEFAULT);
        Bitmap origImage = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        Bitmap image;
        image = addBorder(origImage, 2, Color.WHITE);
        image = addBorder(origImage, 2, Color.BLACK);
        Mat colorImage = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC4);
        Utils.bitmapToMat(image, colorImage);
        ArrayList<MatOfPoint> contours = findContours(colorImage, area);
        if(contours.isEmpty()) {
            callback.invoke("Contour not found");
            return;
        }
        List<Quadrilateral> quads = getQuadrilateral(contours);
        //determining point sets to get the transformation matrix

        WritableArray d = new WritableNativeArray();
        for (Quadrilateral quad : quads) {
            WritableArray array = new WritableNativeArray();
            for (Point p : quad.points) {
                WritableArray a = new WritableNativeArray();
                a.pushDouble(p.x);
                a.pushDouble(p.y);
                array.pushArray(a);
            }
            d.pushArray(array);
        }
        callback.invoke(null, d);
    }

    private ArrayList<MatOfPoint> findContours(Mat src, Double areaThresold) {

        double ratio = src.size().height / 600;
        int height = Double.valueOf(src.size().height / ratio).intValue();
        int width = Double.valueOf(src.size().width / ratio).intValue();
        Size size = new Size(width,height);
        Mat resizedImage = new Mat(size, CvType.CV_8UC4);
        Mat grayImage = new Mat(size, CvType.CV_8UC4);
        Mat cannedImage = new Mat(size, CvType.CV_8UC1);
        Bitmap img = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Imgproc.resize(src,resizedImage,size);
        Imgproc.cvtColor(resizedImage, grayImage, Imgproc.COLOR_RGB2GRAY, 4);
        Imgproc.blur(grayImage, grayImage, new Size(3, 3));
        Utils.matToBitmap(grayImage,img);
//        Imgproc.Canny(grayImage, cannedImage, 75, 200);
        double maxThreshold = 2.5 * 1.0;
        double minThreshold = 7.5 * 1.0;
        // if ostu threshold is enabled, then use it instead
        if (true) {
            Mat tmp2 = new Mat();
            double otsuThreshold = Imgproc.threshold(grayImage, tmp2, (double) 0, (double) 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
            maxThreshold = otsuThreshold;
            minThreshold = otsuThreshold * 0.5;
        }
        // detect & show edges
        Imgproc.Canny(grayImage, cannedImage, minThreshold, maxThreshold);
        Utils.matToBitmap(cannedImage,img);
        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(cannedImage, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        hierarchy.release();
        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
            Imgproc.drawContours(grayImage, contours, contourIdx, new Scalar(38, 255, 0, 1), -1);
        }
        Utils.matToBitmap(grayImage,img);
        Collections.sort(contours, new Comparator<MatOfPoint>() {

            @Override
            public int compare(MatOfPoint lhs, MatOfPoint rhs) {
                return Double.valueOf(Imgproc.contourArea(rhs)).compareTo(Imgproc.contourArea(lhs));
            }
        });
        resizedImage.release();
        grayImage.release();
        cannedImage.release();
        return contours;
    }

    private List<Quadrilateral> getQuadrilateral(ArrayList<MatOfPoint> contours) {
        List<Quadrilateral> rs = new ArrayList<Quadrilateral>();
        int i = 0;
        for ( MatOfPoint c: contours ) {
            MatOfPoint2f c2f = new MatOfPoint2f(c.toArray());
            double peri = Imgproc.arcLength(c2f, true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(c2f, approx, 0.02 * peri, true);
            Point[] points = approx.toArray();
            if (points.length < 4) {
                continue;
            }
                Point[] foundPoints = sortPoints(points);
//                Point[] foundPoints = points;
                rs.add(new Quadrilateral(c, foundPoints));
//            }
            i += 1;
            if( i >= 6) {
                break;
            }
        }

        return rs;
    }

    private Point[] sortPoints(Point[] src) {

        ArrayList<Point> srcPoints = new ArrayList<>(Arrays.asList(src));

        Point[] result = { null , null , null , null };

        Comparator<Point> sumComparator = new Comparator<Point>() {
            @Override
            public int compare(Point lhs, Point rhs) {
                return Double.valueOf(lhs.y + lhs.x).compareTo(rhs.y + rhs.x);
            }
        };

        Comparator<Point> diffComparator = new Comparator<Point>() {

            @Override
            public int compare(Point lhs, Point rhs) {
                return Double.valueOf(lhs.y - lhs.x).compareTo(rhs.y - rhs.x);
            }
        };

        // top-left corner = minimal sum
        result[0] = Collections.min(srcPoints, sumComparator);

        // bottom-right corner = maximal sum
        result[2] = Collections.max(srcPoints, sumComparator);

        // top-right corner = minimal diference
        result[1] = Collections.min(srcPoints, diffComparator);

        // bottom-left corner = maximal diference
        result[3] = Collections.max(srcPoints, diffComparator);

        return result;
    }

    private Bitmap addBorder(Bitmap bmp, int borderSize, int color) {
        Bitmap bmpWithBorder = Bitmap.createBitmap(bmp.getWidth() + borderSize * 2, bmp.getHeight() + borderSize * 2, bmp.getConfig());
        Canvas canvas = new Canvas(bmpWithBorder);
        canvas.drawColor(color);
        canvas.drawBitmap(bmp, borderSize, borderSize, null);
        return bmpWithBorder;
    }
}
