import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import javax.imageio.ImageIO;
import javax.swing.plaf.ColorUIResource;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;

public class App {

    static BufferedImage input_img;
    static BufferedImage output_img;
    static float[][] kernal;
    static int kernal_size;
    static String processing_mode;
    static String kernal_mode;
    static float factor;
    static int offset;
    static int width;
    static int height;
    static String img_name;
    static int finished = 0;

    static String[] _args;

    static Scanner scanner = new Scanner(System.in);

    static float[][] default_kernal = {
            { 0f, 0f, 0f },
            { 0f, 1f, 0f },
            { 0f, 0f, 0f }
    };
    static float[][] sharpen_kernel = {
            { 0f, -1f, 0f },
            { -1f, 5f, -1f },
            { 0f, -1f, 0f }
    };
    static float[][] edge_detection_kernal = {
            { -1f, -1f, -1f },
            { -1f, 8f, -1f },
            { -1f, -1f, -1f }
    };
    static float[][] motion_blur_kernal = {
            { 1f, 0f, 0f, 0f, 0f },
            { 0f, 1f, 0f, 0f, 0f },
            { 0f, 0f, 1f, 0f, 0f },
            { 0f, 0f, 0f, 1f, 0f },
            { 0f, 0f, 0f, 0f, 1f }
    };

    public static void main(String[] args) throws InterruptedException, IOException {
        _args = args;
        set_image();
        set_kernal();
        set_mode();

    }

    // sequential mode
    public static void process_img_sequential(String img_name, float[][] kernal, String kernel_mode, float factor)
            throws IOException {
        // String filePath =
        // "C:/Users/lukaz/Documents/faks21-22/programiranje3/Kernal_Image_Processing/kernal_image_processing_final/KernalImageProcessing/src/images/"
        // + img_name + ".png";
        // URL imageUrl = App.getClass().getResource("/images/"+ img_name +".png");
        // String filePath = "images\\"
        // + img_name + ".png";
        String filePath = "images/" + img_name + (img_name.contains(".png") ? "" : ".png");
        File img_file = new File(filePath);
        System.out.println(img_file);
        // input_img = ImageIO.read(img_file);
        input_img = ImageIO.read(img_file);
        width = input_img.getWidth();
        height = input_img.getHeight();
        int type = input_img.getType();

        offset = kernal.length / 2;

        output_img = new BufferedImage(width, height, type);
        long t0 = System.currentTimeMillis();
        for (int i = offset; i < width - offset; i++) {
            for (int j = offset; j < height - offset; j++) {
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
                // int res_rgb = new Color(red, green, blue).getRGB();

                output_img.setRGB(i, j, res_rgb);
            }
        }
        long t1 = System.currentTimeMillis();

        save_image(img_name, kernel_mode, output_img, (t1 - t0));

    }

    // parallel mode
    public static void process_img_parallel(String img_name, float[][] kernal, String kernel_mode, float factor)
            throws IOException {
        int num_threads = Runtime.getRuntime().availableProcessors();

        File img_file = new File("images/" + img_name + (img_name.contains(".png") ? "" : ".png"));
        System.out.println(img_file);

        input_img = ImageIO.read(img_file);

        width = input_img.getWidth();
        height = input_img.getHeight();
        int type = input_img.getType();
        offset = kernal.length / 2;

        int split = height / num_threads;
        output_img = new BufferedImage(width, height, type);

        long t0 = System.currentTimeMillis();
        ExecutorService executor_service = Executors.newFixedThreadPool(num_threads);
        Runnable worker;
        for (int i = offset; i < height - offset; i += split) {
            if (height - offset - i < split) {
                worker = new Worker(i, height - offset);
            } else {
                worker = new Worker(i, i + split);
            }
            executor_service.execute(worker);
        }
        executor_service.shutdown();
        while (!executor_service.isTerminated()) {
        }
        long t1 = System.currentTimeMillis();
        save_image(img_name, kernel_mode, output_img, (t1 - t0));
    }

    // disctributed mode
    public static void process_img_disctributed(String img_name, float[][] kernal, String kernel_mode, float factor)
            throws IOException {
        File img_file = new File("src\\images\\" + img_name + ".png");

        System.out.println(img_file);

        input_img = ImageIO.read(img_file);

        width = input_img.getWidth();
        height = input_img.getHeight();
        int type = input_img.getType();
        offset = kernal.length / 2;

        output_img = new BufferedImage(width, height, type);

        long t0 = System.currentTimeMillis();
        MPJ_Worker distributed = new MPJ_Worker();
        distributed.main(null);

        while (finished < 4) {
        }
        long t1 = System.currentTimeMillis();

        save_image(img_name, kernel_mode, output_img, (t1 - t0));

    }

    // save image with unique name to results folder
    public static void save_image(String img_name, String kernel_mode, BufferedImage output_img, long time)

            throws IOException {
        int serial_number = 1;
        while (true) {
            String result_name = img_name + "-" + kernel_mode + "-" + serial_number + ".png";
            File result_path = new File(
                    "src/results/" + result_name);
            if (!result_path.exists()) {
                ImageIO.write(output_img, "png", result_path);
                System.out.println("\n");
                System.out.println("Result saved as: " + result_name);
                System.out.println("Time spent computing: " + time + "milliseconds");
                break;
            } else {
                serial_number++;
            }
        }
    }

    // select image
    public static void set_image() {
        // select image
        System.out.println("Enter the name of the image from \"images\" folder:");
        img_name = scanner.nextLine();
    }

    // select custom or predefined kernel
    public static void set_kernal() {
        // select kernel
        System.out.println(
                "Enter the size of the custom kernel that you want to use or write the name of the predefined kernel:");
        System.out.println("- DEFAULT");
        System.out.println("- SHARPEN");
        System.out.println("- EDGE DETECTION");
        System.out.println("- MOTION BLUR");

        String kernal_option = scanner.nextLine();

        switch (kernal_option) {
            case "DEFAULT":
                kernal = default_kernal;
                kernal_mode = "DEFAULT";
                factor = 1f;
                break;
            case "SHARPEN":
                kernal = sharpen_kernel;
                kernal_mode = "SHARPEN";
                factor = 1f;
                break;
            case "EDGE DETECTION":
                kernal = edge_detection_kernal;
                kernal_mode = "EDGE DETECTION";
                factor = 1f;
                break;
            case "MOTION BLUR":
                kernal = motion_blur_kernal;
                kernal_mode = "MOTION BLUR";
                factor = 0.2f;
                break;
            default:
                kernal_size = Integer.parseInt(kernal_option);

                // set size of kernal
                kernal = new float[kernal_size][kernal_size];

                float sum = 0f;

                for (int i = 0; i < kernal_size; i++) {
                    for (int j = 0; j < kernal_size; j++) {
                        kernal[i][j] = scanner.nextFloat();
                        sum += kernal[i][j];
                    }
                }
                kernal_mode = "CUSTOM";
                factor = sum / (kernal_size * kernal_size);
                processing_mode = scanner.nextLine();
                break;
        }
    }

    // set the mode of computation
    public static void set_mode() throws IOException {
        System.out.println("Select the mode of processing that you want to run:");
        System.out.println("- SEQUENTIAL");
        System.out.println("- PARALLEL");
        System.out.println("- DISTRIBUTED");

        processing_mode = scanner.nextLine();

        switch (processing_mode) {
            case "SEQUENTIAL":
                process_img_sequential(img_name, kernal, kernal_mode, factor);
                break;
            case "PARALLEL":
                process_img_parallel(img_name, kernal, kernal_mode, factor);
                break;
            case "DISTRIBUTED":
                process_img_disctributed(img_name, kernal, kernal_mode, factor);
                break;
        }

    }
}
