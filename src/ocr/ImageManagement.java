package ocr;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

public final class ImageManagement {
	public static class Loader {
		/**
		 * @param file
		 *            The image file to load into memory
		 * @param comp
		 *            Some subclass of component, for the MediaTracker which
		 *            watches the loading. God knows why it needs one.
		 * @return A writeable raster copy of the image.
		 */
		public static WritableRaster loadImgToRaster(String file, Component comp) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			Image img = tk.getImage(file);
			MediaTracker mt = new MediaTracker(comp);
			mt.addImage(img, 1);
			try {
				mt.waitForAll();
			} catch (InterruptedException e) {
				System.out.println(e.getMessage());
			}
			int w = img.getWidth(comp);
			int h = img.getHeight(comp);
			int[] pixels = new int[w * h];
			PixelGrabber pg = new PixelGrabber(img, 0, 0, w, h, pixels, 0, w);
			try {
				pg.grabPixels();
			} catch (InterruptedException e) {
				System.out.println(e);
			}
			DataBufferInt db = new DataBufferInt(pixels, (w * h), 0);
			int[] bm = new int[] { 0xff0000, 0xff00, 0xff, 0xff000000 };
			SinglePixelPackedSampleModel sm = new SinglePixelPackedSampleModel(DataBuffer.TYPE_INT, w, h, bm);
			return WritableRaster.createWritableRaster(sm, db, null);
		}

		/**
		 * @param file
		 *            The file to serialise the corners to
		 * @param corners
		 *            An int[4][2] containing four corner points comprised of
		 *            x/y components
		 */
		public static void saveCorners(String file, int[][] corners) {
			try {
				ObjectOutput out = new ObjectOutputStream(new FileOutputStream(file));
				out.writeObject(corners);
				out.close();
			} catch (IOException e2) {
				System.err.println(e2);
			}
		}

		/**
		 * @param file
		 *            The file to load the seralised files from
		 * @return An int[4][2] containing four corner points comprised of x/y
		 *         components
		 */
		public static int[][] loadCorners(String file) {
			int[][] out = null;
			try {
				File f = new File(file);
				ObjectInputStream in = new ObjectInputStream(new FileInputStream(f));
				out = (int[][]) in.readObject();
				in.close();
			} catch (IOException e) {
				System.err.println(e);
			} catch (ClassNotFoundException e) {
				System.err.println(e);
			}
			return out;
		}
	}

	public static File getLatestJPG(String rootStr) throws FileNotFoundException {
		File handle = new File(rootStr);
		if (!handle.isDirectory()) {
			throw new FileNotFoundException("Invalid path");
		}
		File picture = null;
		Long latest = new Long(0);
		for (File f : handle.listFiles()) {
			String name = f.getName().toLowerCase();
			Long mod = f.lastModified();
			if (name.contains(".jpg") && mod > latest) {
				latest = mod;
				picture = f;
			}
		}
		if (picture == null) {
			throw new FileNotFoundException("No pictures present in folder");
		} else {
			return picture;
		}
	}

	/**
	 * @param wr
	 *            Within the bounds of this raster, set any white pixel which is
	 *            not reachable from the centre most white pixel to black
	 */
	public static void magicWand(WritableRaster wr) {
		int w = wr.getWidth();
		int h = wr.getHeight();
		int tagged = 0;
		while (tagged < w * h * 0.05) {
			// Find the centremost whitemost pixel, circling out from the mid
			// point, start at the middle
			Point mid = new Point(w / 2, h / 2);
			// Number of moves
			int n = 1;
			// Direction: up, right, down, left
			int d = 0;
			// Accumulator
			int a = 0;
			// Run this loop to search for a white pixel.
			// The green check makes sense on inspection of wandLoop which may
			// recall this method.
			while (isBlack(wr, mid.x, mid.y) || isGreen(wr, mid.x, mid.y)) {
				switch (d) {
				case (0):
					mid.y--;
					break;
				case (1):
					mid.x++;
					break;
				case (2):
					mid.y++;
					break;
				case (3):
					mid.x--;
					break;
				}
				a++;
				// Change direction, reset accumulator
				if (a == n) {
					d = (d + 1) % 4;
					a = 0;
				}
				// Inc. number of moves to make in one direction
				if (a == 0 && (d == 0 || d == 2)) {
					n++;
				}
			}
			tagged += wandLoop(wr, mid.x, mid.y);
		}
		// Once we're satifsfied we blacken all non green and set green to pure
		// white
		for (int r = 0; r < h; r++) {
			for (int c = 0; c < w; c++) {
				if (isGreen(wr, c, r)) {
					wr.setPixel(c, r, new int[] { 255, 255, 255, 255 });
				} else {
					wr.setPixel(c, r, new int[] { 0, 0, 0, 255 });
				}
			}
		}
	}

	/**
	 * @param wr
	 *            Raster to operate on
	 * @param x
	 * @param y
	 */
	private static int wandLoop(WritableRaster wr, int x, int y) {
		int w = wr.getWidth();
		int h = wr.getHeight();
		// We record which pixels are reachable from the midpoint by marking
		// them green
		int[] green = { 0, 255, 0, 255 };
		int tagged = 0;
		// Now traverse the current row, left then right until a non black pixel
		// is found
		int l, r;
		l = r = x;
		do {
			wr.setPixel(l, y, green);
			tagged++;
			l--;
		} while (l > 0 && isWhite(wr, l, y) && !isGreen(wr, l, y));
		l++;
		do {
			wr.setPixel(r, y, green);
			tagged++;
			r++;
		} while (r < w && isWhite(wr, r, y) && !isGreen(wr, r, y));
		r--;
		// Check pixels above, then below this l to r on this row
		for (int i = l; i <= r; i++) {
			if (y > 0 && isWhite(wr, i, y - 1) && !isGreen(wr, i, y - 1)) {
				tagged += wandLoop(wr, i, y - 1);
			}
			if (y < h && isWhite(wr, i, y + 1) && !isGreen(wr, i, y + 1)) {
				tagged += wandLoop(wr, i, y + 1);
			}
		}
		return tagged;
	}

	/**
	 * @param wr
	 *            Adjust the bounds of this rasters sample model to encroach
	 *            until a decent block of white is found. Descent is defined in
	 *            the method.
	 */
	public static WritableRaster autoCrop(WritableRaster wr) {
		int w = wr.getWidth();
		int h = wr.getHeight();
		int top = 0;
		int bottom = h;
		int left = 0;
		int right = w;
		int hMinThick = 2;
		int vMinThick = 2;
		float minWhite = 0.01f;
		// Horizontal scan to idenfity rows which contain more than a factor of
		// minWhite white pixels
		boolean[] hWhite = new boolean[h];
		for (int r = 0; r < h; r++) {
			int white = 0;
			for (int c = 0; c < w; c++) {
				if (isWhite(wr, c, r)) {
					white++;
				}
			}
			if (white > w * minWhite) {
				hWhite[r] = true;
			} else {
				hWhite[r] = false;
			}
		}
		// Locate top most row before hMinThick consecutive white lines
		int tagged = 0;
		for (int r = 0; r < h; r++) {
			if (tagged == hMinThick) {
				top = r - hMinThick;
				break;
			}
			if (hWhite[r]) {
				tagged++;
			} else {
				tagged = 0;
			}
		}
		// Locate bottom most row before hMinThick consecutive white lines
		tagged = 0;
		for (int r = h - 1; r >= 0; r--) {
			if (tagged == hMinThick) {
				bottom = r + hMinThick;
				break;
			}
			if (hWhite[r]) {
				tagged++;
			} else {
				tagged = 0;
			}
		}
		// Horizontal scan to idenfity row which contain more than a factor of
		// minWhite white pixels
		boolean[] vWhite = new boolean[w];
		for (int c = 0; c < w; c++) {
			int white = 0;
			for (int r = 0; r < h; r++) {
				if (isWhite(wr, c, r)) {
					white++;
				}
			}
			if (white > h * minWhite) {
				vWhite[c] = true;
			} else {
				vWhite[c] = false;
			}
		}
		// Locate left most column before vMinThick consecutive white lines
		tagged = 0;
		for (int c = 0; c < w; c++) {
			if (tagged == vMinThick) {
				left = c - vMinThick;
				break;
			}
			if (vWhite[c]) {
				tagged++;
			} else {
				tagged = 0;
			}
		}
		// Locate right most column before vMinThick consecutive white lines
		tagged = 0;
		for (int c = w - 1; c >= 0; c--) {
			if (tagged == vMinThick) {
				right = c + vMinThick;
				break;
			}
			if (vWhite[c]) {
				tagged++;
			} else {
				tagged = 0;
			}
		}
		int w2 = right - left;
		int h2 = bottom - top;
		// Choose longest side between width and height
		try {
			if (w2 > h2) {
				return wr.createWritableChild(left, top - ((w2 - h2) / 2), w2, w2, left, top - ((w2 - h2) / 2), null);
			} else {
				return wr.createWritableChild(left - ((h2 - w2) / 2), top, h2, h2, left - ((h2 - w2) / 2), top, null);
			}
		} catch (RasterFormatException e) {
			return wr.createWritableChild(left, top, w2, h2, left, top, null);
		}
	}

	/**
	 * @param r
	 *            The raster to check
	 * @param x
	 *            The x coord of the pixel to check
	 * @param y
	 *            The y coord of the pixel to check
	 * @return True if the average value of the RGB components is below half
	 *         (128)
	 */
	private static boolean isBlack(Raster r, int x, int y) {
		try {
			int[] pixel = r.getPixel(x, y, (int[]) null);
			return (pixel[0] + pixel[1] + pixel[2]) / 3 < 128;
		} catch (ArrayIndexOutOfBoundsException e) {
			return true;
		}
	}

	/**
	 * @param r
	 *            The raster to check
	 * @param x
	 *            The x coord of the pixel to check
	 * @param y
	 *            The y coord of the pixel to check
	 * @return True if the average value of the RGB components is above or equal
	 *         to half (128)
	 */
	private static boolean isWhite(Raster r, int x, int y) {
		return !isBlack(r, x, y);
	}

	/**
	 * @param r
	 *            The raster to check
	 * @param x
	 *            The x coord of the pixel to check
	 * @param y
	 *            The y coord of the pixel to check
	 * @return True if the specified pixel is pure green
	 */
	private static boolean isGreen(Raster r, int x, int y) {
		int[] p = r.getPixel(x, y, (int[]) null);
		int[] g = new int[] { 0, 255, 0, 255 };
		return p[0] == g[0] && p[1] == g[1] && p[2] == g[2] && p[3] == g[3];
	}

	/**
	 * @param wr
	 *            Within the bounds of this raster, set any pixels not
	 *            considered white to black. The conditions for being a white
	 *            pixel are defined within the method.
	 */
	public static void extWhite(WritableRaster wr) {
		int w = wr.getWidth();
		int h = wr.getHeight();
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				int[] hsv = argbToHSV(wr, x, y);
				if (!(hsv[1] < 40 && hsv[2] > 50)) { // 30/45 for TFT
					wr.setPixel(x, y, new int[] { 0, 0, 0, 255 });
				}
			}
		}
	}

	/**
	 * @param ras
	 *            The raster containing the target pixel
	 * @param x
	 *            Pixel's x coord
	 * @param y
	 *            Pixel's y coord
	 * @return The hue, saturation and value colour space values for the
	 *         specified pixel, in that order.
	 */
	private static int[] argbToHSV(Raster ras, int x, int y) {
		int[] pixel = ras.getPixel(x, y, (int[]) null);
		float r = pixel[0] / 255f;
		float g = pixel[1] / 255f;
		float b = pixel[2] / 255f;
		float max = Math.max(Math.max(r, g), b);
		float min = Math.min(Math.min(r, g), b);
		float h = 0;
		if (r == max && g >= b) {
			h = ((g - b) / (max - min)) * 60;
		} else if (r == max && g < b) {
			h = (((g - b) / (max - min)) * 60) + 360;
		} else if (g == max) {
			h = (((b - r) / (max - min)) * 60) + 120;
		} else if (b == max) {
			h = (((r - g) / (max - min)) * 60) + 240;
		}
		int s = 0;
		if (max != 0) {
			s = Math.round(((max - min) / max) * 100);
		}
		int v = Math.round(max * 100);
		return new int[] { Math.round(h), s, v };
	}

	/**
	 * @param wr
	 *            Attempts to change any pixels on this raster which may be
	 *            white due to tile highlights to black. This is done by looking
	 *            for rows and column which contain a high number of white
	 *            pixels, high is defined in the method.
	 */
	public static void remHighlights(WritableRaster wr) {
		int w = wr.getWidth();
		int h = wr.getHeight();
		// Row / col is set to black is more than this factor of its pixels are
		// white.
		float rWhite = 0.60f;
		float cWhite = 0.85f;
		int tagged;
		// Scan rows
		for (int r = 0; r < h; r++) {
			tagged = 0;
			for (int c = 0; c < w; c++) {
				if (isWhite(wr, c, r)) {
					tagged++;
				}
			}
			if (tagged > w * rWhite) {
				for (int c = 0; c < w; c++) {
					wr.setPixel(c, r, new int[] { 0, 0, 0, 255 });
				}
			}
		}
		// Scan columns
		for (int c = 0; c < w; c++) {
			tagged = 0;
			for (int r = 0; r < h; r++) {
				if (isWhite(wr, c, r)) {
					tagged++;
				}
			}
			if (tagged > h * cWhite) {
				for (int r = 0; r < h; r++) {
					wr.setPixel(c, r, new int[] { 0, 0, 0, 255 });
				}
			}
		}
	}

	/**
	 * @param bands
	 *            An array indicating the split points for each colour band
	 * @param r
	 *            A raster who's bounds cover one tile
	 * @return The calculated colour of the tile based on the hue values defined
	 *         within the method
	 */
	public static int tileColour(int[] bands, Raster r) {
		final int[] co = new int[] {6,4,0,1,2,3,5,6};
		int[] pCount = { 0, 0, 0, 0, 0, 0, 0 };
		final int w = r.getWidth();
		final int h = r.getHeight();
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				int[] hsv = argbToHSV(r, x, y);
				// First check if the pixel is reasonably saturated
				if (hsv[1] > 50) {
					int col = hsv[0];
					for (int i = 0; i < 8; i++) {
						if(col < bands[i]) {
							pCount[co[i]]++;
							break;
						}
					}
				}
			}
		}
		int winner = 0;
		int max = 0;
		for (int i = 0; i < 7; i++) {
			if (pCount[i] > max) {
				winner = i;
				max = pCount[i];
			}
		}
		return winner;
	}

	/**
	 * @param corners
	 *            A set of 4 integer tuples indicating the corners of the grid
	 *            in the given Raster
	 * @param wr
	 *            A raster holding the databuffer for the picture of the grid
	 * @return An array of 117 rasters all using the databuffer from the given
	 *         Raster but with there sample model bounds adjusted to cover each
	 *         tile. Array ordering is row by row, right to left.
	 */
	public static WritableRaster[] makeRasterTiles(int[][] corners, WritableRaster wr) {
		int[] tl = corners[0];
		int[] tr = corners[1];
		int[] br = corners[2];
		int[] bl = corners[3];
		float ratio = wr.getWidth() / 640;
		float w1 = ((tr[0] - tl[0]) / 13) * ratio;
		float w2 = ((br[0] - bl[0]) / 13) * ratio;
		float h1 = ((bl[1] - tl[1]) / 9) * ratio;
		float h2 = ((br[1] - tr[1]) / 9) * ratio;
		int w = Math.round((w1 + w2) / 2);
		int h = Math.round((h1 + h2) / 2);
		int tlX = Math.round(tl[0] * ratio);
		int tlY = Math.round(tl[1] * ratio);
		WritableRaster[] out = new WritableRaster[117];
		for (int r = 0; r < 9; r++) {
			for (int c = 0; c < 13; c++) {
				int x = tlX + (c * w);
				int y = tlY + (r * h);
				out[(r * 13) + c] = wr.createWritableChild(x, y, w, h, 0, 0, null);
			}
		}
		return out;
	}

	/**
	 * @param r
	 *            Pixel set to be converted to a neural net input.
	 * @param nImp
	 *            The number of network inputs to generate from the raster
	 *            (generally some factor of raster width times height).
	 * @return An array of doubles valued either 1 for a white pixel or -1 for a
	 *         black one. Array ordering is row by row, right to left.
	 */
	public static double[] rasterToNetInput(Raster r, int nInp) {
		// Work out ratio for scaling the raster
		int w = r.getWidth();
		int h = r.getHeight();
		double sx = Math.sqrt(nInp) / w;
		double sy = Math.sqrt(nInp) / h;
		// Set up the raster scaling operation and build scaled instance
		AffineTransform at = new AffineTransform();
		at.setToScale(sx, sy);
		AffineTransformOp ato = new AffineTransformOp(at, null);
		Raster scaledR = ato.filter(r, null);
		assert scaledR.getWidth() * scaledR.getHeight() == nInp;
		// Generate the list of outputs.
		// 1 for a white pixel and 0 for a black pixel.
		double[] out = new double[nInp];
		for (int i = 0; i < nInp; i++) {
			// out[i] = argbToHSV(scaledR, i % scaledR.getWidth(), i /
			// scaledR.getWidth())[2] / 100;
			if (isWhite(scaledR, i % scaledR.getWidth(), i / scaledR.getWidth())) {
				out[i] = 1;
			} else {
				out[i] = 0;
			}
		}
		return out;
	}

	public static void printNetInput(double[] inp, int nInp) {
		long s = Math.round(Math.sqrt(nInp));
		for (int i = 0; i < nInp; i++) {
			if (i % s == 0) {
				System.out.println();
			}
			if (inp[i] > 0.5) {
				System.out.print("#");
			} else {
				System.out.print(".");
			}
		}
		System.out.println();
		System.out.println();
	}
}
