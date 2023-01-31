import mpi.*;
import java.awt.Color;
import java.awt.image.BufferedImage;

public class MPJ_Worker {
	
	static int height = App.height;
	static int width = App.width;
	static int offset = App.offset;
	static float[][] kernal = App.kernal;
	static BufferedImage input_img = App.input_img;
	static BufferedImage output_img = App.output_img;
	static float factor = App.factor;
			
	
	public static void main(String[] args) {
		MPI.Init(args);
		int me = MPI.COMM_WORLD.Rank();
		int size = MPI.COMM_WORLD.Size();
		
		int split = height / size;
		
		int[] startStop = new int[2];
		
		if(me == 0) {
			for (int i = offset; i < height - offset; i += split) {
				
	            if (height - offset - i < split) {
	            	startStop[0] = i;
	            	startStop[1] = height - offset;
	            	System.out.println("sending "+i);
	            	MPI.COMM_WORLD.Send(startStop, 0, 2, MPI.INT, i, 0);
	                //worker = new Worker(i, height - offset);
	            } else {
	            	startStop[0] = i;
	            	startStop[1] = i + split;
	            	System.out.println("sending "+i);
	            	MPI.COMM_WORLD.Send(startStop, 0, 2, MPI.INT, i, 0);
	                //worker = new Worker(i, i + split);
	            }
	            
	        }
		}else {
			MPI.COMM_WORLD.Recv(startStop, 0, 2, MPI.INT, 0, 0);
			int start = startStop[0];
			int stop = startStop[1];
			
			for (int i = offset; i < width - offset; i++) {
	            for (int j = start; j < stop; j++) {
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
		MPI.Finalize();
		App.finished += 1;
		System.out.println(""+App.finished);
	}
}
