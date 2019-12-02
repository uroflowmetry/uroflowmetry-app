package com.uroflowmetry.library.utils;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {
    public static Bitmap getBitmap(Context context, int drawableRes) {
        Drawable drawable = context.getResources().getDrawable(drawableRes);
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static String getRealPathFromURI(Activity activity, Uri contentUri) {

        // can post image
        String[] proj={MediaStore.Images.Media.DATA};
        @SuppressWarnings("deprecation")
        Cursor cursor = activity.managedQuery( contentUri,
                proj, // Which columns to return
                null,       // WHERE clause; which rows to return (all rows)
                null,       // WHERE clause selection arguments (none)
                null); // Order-by clause (ascending by name)
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();

        return cursor.getString(column_index);
    }

    public static void convertYUV420SPToARGB8888(byte[] input, int width, int height, int[] output) {
        final int frameSize = width * height;
        for (int j = 0, iyp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width;
            int u = 0;
            int v = 0;

            int oyp = height - j - 1;
            for (int i = 0; i < width; i++, iyp++) {
                int y = 0xff & input[iyp];
                if ((i & 1) == 0) {
                    v = 0xff & input[uvp++];
                    u = 0xff & input[uvp++];
                }

                output[oyp + i * height] = YUV2RGB(y, u, v);
                //output[iyp] = YUV2RGB(y, u, v);
            }
        }
    }

    public static void convertYUV420ToARGB8888(
            byte[] yData,
            byte[] uData,
            byte[] vData,
            int width,
            int height,
            int yRowStride,
            int uvRowStride,
            int uvPixelStride,
            int[] out) {
        int yp = 0;
        for (int j = 0; j < height; j++) {
            int pY = yRowStride * j;
            int pUV = uvRowStride * (j >> 1);

            for (int i = 0; i < width; i++) {
                int uv_offset = pUV + (i >> 1) * uvPixelStride;

                out[yp++] = YUV2RGB(0xff & yData[pY + i], 0xff & uData[uv_offset], 0xff & vData[uv_offset]);
            }
        }
    }

    static final int kMaxChannelValue = 262143;
    private static int YUV2RGB(int y, int u, int v) {
        // Adjust and check YUV values
        y = (y - 16) < 0 ? 0 : (y - 16);
        u -= 128;
        v -= 128;

        // This is the floating point equivalent. We do the conversion in integer
        // because some Android devices do not have floating point in hardware.
        // nR = (int)(1.164 * nY + 2.018 * nU);
        // nG = (int)(1.164 * nY - 0.813 * nV - 0.391 * nU);
        // nB = (int)(1.164 * nY + 1.596 * nV);
        int y1192 = 1192 * y;
        int r = (y1192 + 1634 * v);
        int g = (y1192 - 833 * v - 400 * u);
        int b = (y1192 + 2066 * u);

        // Clipping RGB values to be inside boundaries [ 0 , kMaxChannelValue ]
        r = r > kMaxChannelValue ? kMaxChannelValue : (r < 0 ? 0 : r);
        g = g > kMaxChannelValue ? kMaxChannelValue : (g < 0 ? 0 : g);
        b = b > kMaxChannelValue ? kMaxChannelValue : (b < 0 ? 0 : b);

        return 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
    }

    public static Bitmap rotateBitmap(Bitmap bitmapOrg, int degree){
        int width = bitmapOrg.getWidth();
        int height = bitmapOrg.getHeight();

        Matrix matrix = new Matrix();
        matrix.preRotate(degree);

        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmapOrg, 0, 0, width, height, matrix, true);

        return rotatedBitmap;
    }

    public synchronized static Bitmap getSafeDecodeBitmap(String strFilePath, int maxSize) {
        try {
            if (strFilePath == null)
                return null;
            // Max image size
            int IMAGE_MAX_SIZE = maxSize;

            File file = new File(strFilePath);
            if (file.exists() == false) {
                //DEBUG.SHOW_ERROR(TAG, "[ImageDownloader] SafeDecodeBitmapFile : File does not exist !!");
                return null;
            }

            BitmapFactory.Options bfo 	= new BitmapFactory.Options();
            bfo.inJustDecodeBounds 		= true;

            BitmapFactory.decodeFile(strFilePath, bfo);

            if (IMAGE_MAX_SIZE > 0)
                if(bfo.outHeight * bfo.outWidth >= IMAGE_MAX_SIZE * IMAGE_MAX_SIZE) {
                    bfo.inSampleSize = (int) Math.pow(2, (int) Math.round(Math.log(IMAGE_MAX_SIZE
                            / (double) Math.max(bfo.outHeight, bfo.outWidth)) / Math.log(0.5)));
                }
            bfo.inJustDecodeBounds = false;
            bfo.inPurgeable = true;
            bfo.inDither = true;

            final Bitmap bitmap = BitmapFactory.decodeFile(strFilePath, bfo);

            int degree = GetExifOrientation(strFilePath);

            return GetRotatedBitmap(bitmap, degree);
        }
        catch(OutOfMemoryError ex)
        {
            ex.printStackTrace();

            return null;
        }
    }

    private synchronized static int GetExifOrientation(String filepath) 	{
        int degree = 0;
        ExifInterface exif = null;

        try    {
            exif = new ExifInterface(filepath);
        } catch (IOException e)  {
            Log.e("StylePhoto", "cannot read exif");
            e.printStackTrace();
        }

        if (exif != null) {
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);

            if (orientation != -1) {
                // We only recognize a subset of orientation tag values.
                switch(orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        degree = 90;
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_180:
                        degree = 180;
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_270:
                        degree = 270;
                        break;
                }
            }
        }

        return degree;
    }

    private synchronized static Bitmap GetRotatedBitmap(Bitmap bitmap, int degrees) 	{
        if ( degrees != 0 && bitmap != null )     {
            Matrix m = new Matrix();
            m.setRotate(degrees, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2 );
            try {
                Bitmap b2 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
                if (bitmap != b2) {
                    bitmap.recycle();
                    bitmap = b2;
                }
            } catch (OutOfMemoryError ex) {
                // We have no memory to rotate. Return the original bitmap.
            }
        }

        return bitmap;
    }

    private static final String TAG = ImageUtils.class.getSimpleName();

    /**
     * Exif（Exchangeable Image File，可交换图像文件）是一种图像文件格式，它的数据存储与JPEG格式是完全相同的。
     * 实际上，Exif格式就是在JPEG格式头部插入了数码照片的信息，包括拍摄时的光圈、快门、白平衡、ISO、焦距、日期时间等
     * 各种和拍摄条件以及相机品牌、型号、色彩编码、拍摄时录制的声音以及全球定位系统（GPS）、缩略图等。
     * 简单地说，Exif=JPEG+拍摄参数。因此，你可以利用任何可以查看JPEG文件的看图软件浏览Exif格式的照片，
     * 但并不是所有的图形程序都能处理Exif信息。
     * @param imageFilePath 图片路径
     * @return
     * @throws IOException
     */
    public static ExifInterface getExifInterface(String imageFilePath) throws IOException {
        return new ExifInterface(imageFilePath);
    }

    /**
     * 获取图片的宽度和高度
     * @param imageFilePath 图片的路径
     * @return  (width, height)，分别为宽度和高度
     */
    public static Point getWidthAndHeight(String imageFilePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        // 不去真的解析图片，只获取图片头部信息
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(imageFilePath, options);

        int width = options.outWidth;
        int height = options.outHeight;

        return new Point(width, height);
    }

    public static int getBitmapSize(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { // API 19
            return bitmap.getAllocationByteCount();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {// API 12
            return bitmap.getByteCount();
        }

        return bitmap.getRowBytes() * bitmap.getHeight();
    }

    /**
     * 获取图片的宽度和高度
     * @param is 图片的文件流
     * @return   (width, height)，分别为宽度和高度
     */
    public static Point getWidthAndHeight(InputStream is) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        // 不去真的解析图片，只获取图片头部信息
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeStream(is, null, options);

        int width = options.outWidth;
        int height = options.outHeight;

        return new Point(width, height);
    }



    /**
     * 图片缩放
     * @param bitmap    目标图片
     * @param desWidth  目标宽度
     * @param desHeight 目标高度
     */
    public static Bitmap zoomBitmap(Bitmap bitmap, int desWidth, int desHeight) {
        return zoomBitmap(bitmap, (float) desWidth / bitmap.getWidth(),
                (float) desHeight / bitmap.getHeight());
    }

    /**
     * 图片缩放
     */
    public static Bitmap zoomBitmap(Bitmap bitmap, float sx, float sy) {
        Matrix matrix = new Matrix();
        matrix.postScale(sx, sy);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);
    }

    public static byte[] compress(Bitmap bitmap, Bitmap.CompressFormat format) {
        return compress(bitmap, 75, format);
    }

    public static byte[] compress(Bitmap bitmap, int quality, Bitmap.CompressFormat format) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(format, quality, baos);
        return baos.toByteArray();
    }

    public static boolean saveBitmapInFile(Bitmap bitmap, String fileDir, String fileName, Bitmap.CompressFormat format) {
        //return saveBitmapToFile(bitmap, filePath, 75, format);
//        string filePath = Environment.getExternalStorageDirectory().getAbsolutePath() +
//                "/PhysicsSketchpad";
        boolean bRet = false;
        File dir = new File(fileDir);
        if(!dir.exists())
            dir.mkdirs();

        try {
            File file = new File(dir, fileName);
            FileOutputStream fOut = new FileOutputStream(file);

            bitmap.compress(Bitmap.CompressFormat.PNG, 85, fOut);
            fOut.flush();
            fOut.close();
            bRet = true;
        }catch (Exception e){

        }
        return bRet;
    }


    public static boolean saveBitmapToFile(Bitmap bitmap, String filePath, int quality, Bitmap.CompressFormat format) {
        FileOutputStream fos = null;
        try {
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            fos = new FileOutputStream(file);
            fos.write(compress(bitmap, quality, format));
            return true;
        } catch (Exception e) {
            Log.e(TAG, "saveBitmapToFile()", e);
            return false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.e(TAG, "saveBitmapToFile()", e);
                }
            }
        }
    }

    public static Bitmap toBitmap(byte[] data) {
        return BitmapFactory.decodeByteArray(data, 0, data.length);
    }

    public static Bitmap loadBitmapFromView(View v) {
        Bitmap b = Bitmap.createBitmap( v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        v.layout(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
        v.draw(c);
        return b;
    }

    public static Matrix getTransformationMatrix(
            final int srcWidth,
            final int srcHeight,
            final int dstWidth,
            final int dstHeight,
            final int applyRotation,
            final boolean maintainAspectRatio) {
        final Matrix matrix = new Matrix();

        if (applyRotation != 0) {
            if (applyRotation % 90 != 0) {
                //LOGGER.w("Rotation of %d % 90 != 0", applyRotation);
            }

            // Translate so center of image is at origin.
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);

            // Rotate around origin.
            matrix.postRotate(applyRotation);
        }

        // Account for the already applied rotation, if any, and then determine how
        // much scaling is needed for each axis.
        final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;

        final int inWidth = transpose ? srcHeight : srcWidth;
        final int inHeight = transpose ? srcWidth : srcHeight;

        // Apply scaling if necessary.
        if (inWidth != dstWidth || inHeight != dstHeight) {
            final float scaleFactorX = dstWidth / (float) inWidth;
            final float scaleFactorY = dstHeight / (float) inHeight;

            if (maintainAspectRatio) {
                // Scale by minimum factor so that dst is filled completely while
                // maintaining the aspect ratio. Some image may fall off the edge.
                final float scaleFactor = Math.max(scaleFactorX, scaleFactorY);
                matrix.postScale(scaleFactor, scaleFactor);
            } else {
                // Scale exactly to fill dst from src.
                matrix.postScale(scaleFactorX, scaleFactorY);
            }
        }

        if (applyRotation != 0) {
            // Translate back from origin centered reference to destination frame.
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }

        return matrix;
    }
}
