import java.awt.Color;
import java.awt.image.BufferedImage;

public class Worker implements Runnable {

    int start;
    int end;

    public Worker(int start, int end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public void run() {

        int offset = App.offset;
        float[][] kernal = App.kernal;
        int width = App.width;
        int height = App.height;
        BufferedImage input_img = App.input_img;
        BufferedImage output_img = App.output_img;
        float factor = App.factor;

        for (int i = offset; i < width - offset; i++) {
            for (int j = start; j < end; j++) {
                float red = 0f;
                float green = 0f;
                float blue = 0f;
                for (int k = 0; k < kernal.length; k++) {
                    for (int k2 = 0; k2 < kernal.length; k2++) {
                        int x = i + k - offset;
                        int y = j + k2 - offset;

                        int rgb = input_img.getRGB(x, y);
                        int r = (rgb >> 16) & 0xff;
                        int g = (rgb >> 8) & 0xff;
                        int b = (rgb) & 0xff;

                        red += (r * kernal[k][k2] * factor);
                        green += (g * kernal[k][k2] * factor);
                        blue += (b * kernal[k][k2] * factor);
                    }
                }
                int res_red = Math.min(Math.max((int) red, 0), 255);
                int res_green = Math.min(Math.max((int) green, 0), 255);
                int res_blue = Math.min(Math.max((int) blue, 0), 255);

                int res_rgb = new Color(res_red, res_green, res_blue).getRGB();

                output_img.setRGB(i, j, res_rgb);
            }
        }
    }
}
