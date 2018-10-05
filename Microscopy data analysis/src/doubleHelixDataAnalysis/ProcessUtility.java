package doubleHelixDataAnalysis;

import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.swing.JProgressBar;

import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;

import miatool.miamain.miatoolconfiguration.preferences.Preference;
import miatool.tools.viewer.DisplayController;
import miatool.tools.viewer.GLDisplayViewer;

public class ProcessUtility extends GLDisplayViewer {
	protected int maxWidth;
	protected int maxHeight;
	private final JProgressBar progressBar = new JProgressBar();

	@Preference
	protected String saveLocation;

	@Preference
	protected int windowX;

	@Preference
	protected int windowY;

	@Preference
	protected int windowW;

	@Preference
	protected int windowH;

	@Preference
	protected boolean visible;

	@Preference
	protected int exportMode;

	@Preference
	protected double scalingFactor;

	public ProcessUtility(DisplayController dcontrol) {
		super(dcontrol, null, 0);
	}

	/**
	 * Exports an image containing all viewers
	 *
	 * @param nviews
	 *        the number of viewers
	 */
	public BufferedImage exportMontage(final int nviews) {
		// create a new off screen renderer
		displayUtil.createOffscreenRenderer(1, 1);

		viewerProperties.setZoom(scalingFactor);
		double zoom = viewerProperties.getZoomFactor();

		// get the layout for the window
		GridLayout glo = displayController.getDisplayModeLayout(0);

		int [] iwh = getMaxViewportSize();
		final int iw = (int) (iwh[0] * zoom);
		final int ih = (int) (iwh[1] * zoom);

		final int cols = glo.getColumns();
		final int rows = glo.getRows();

		final int fw = iw * cols;
		final int fh = ih * rows;

		progressBar.setMaximum(nviews);
		progressBar.setValue(0);

		BufferedImage fullImg;
		fullImg = new BufferedImage(fw, fh,
				BufferedImage.TYPE_INT_ARGB);

		BufferedImage img;
		int ix, iy;

		int vindex = index;

		for (int i = 0; i < nviews; i++) {
			ix = i % cols;
			iy = i / cols;

			index = i;

			img = render();

			fullImg.getGraphics().drawImage(img,
					ix * iw,
					iy * ih, img.getWidth(), img.getHeight(),
					null);
			progressBar.setValue(i + 1);
		}

		index = vindex;

		String path = "C:\\Users\\milad\\Desktop\\movieexportTest";
		File imgFile = new File(path);

		if (imgFile.exists()) {
			imgFile.delete();
		}
		return fullImg;
	}

	@Override
	public int getHeight() {
		return maxHeight;
	}

	/**
	 * @return the maximum width and height of all the textures currently
	 *         available in all viewers
	 *
	 *         <pre>
	 * int [] { WIDTH, HEIGHT }
	 *         </pre>
	 */
	protected int [] getMaxViewportSize() {
		int w, h;
		int nwindows = displayController.getNumWindows();

		if (nwindows < 1) {
			return new int [] { 0, 0 };
		}

		int nviews = displayController.getNumViewers(0);

		w = 0;
		h = 0;

		int cw, ch;

		int vindex = index;

		for (int i = 0; i < nviews; i++) {
			index = i;
			displayController.updateTextures(this);

			cw = textures.getMaxViewportWidth();
			ch = textures.getMaxViewportHeight();

			w = w < cw ? cw : w;
			h = h < ch ? ch : h;
		}

		index = vindex;
		return new int [] { w, h };
	}

	@Override
	public int getWidth() {
		return maxWidth;
	}

	/**
	 * @return the BufferedImage containing the image rendered according to the
	 *         fields in the tool
	 */
	public BufferedImage render() {
		// create a new off screen renderer
		displayUtil.createOffscreenRenderer(1, 1);

		// Now prepare to display the textures.
		viewerProperties.setZoom(scalingFactor);
		final double zoom = viewerProperties.getZoomFactor();

		clearTextures();

		// get all textures ready to draw
		displayController.updateTextures(this);

		int [] maxvport = getMaxViewportSize();

		maxWidth = (int) (maxvport[0] * zoom);
		maxHeight = (int) (maxvport[1] * zoom);

		if (maxWidth == 0 || maxHeight == 0) {
			return null;
		}

		// create a new off screen renderer
		displayUtil.createOffscreenRenderer(maxWidth, maxHeight);

		GL2 gl = displayUtil.getGLObject();
		gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_FASTEST);
		// Set up the canvas for rendering OpenGL images
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrtho(0, maxWidth, maxHeight, 0, -1, 1);
		gl.glViewport(0, 0, maxWidth, maxHeight);

		gl.glClear(GL.GL_COLOR_BUFFER_BIT);
		gl.glMatrixMode(GL2.GL_MODELVIEW);

		// get all textures ready to draw
		displayController.updateTextures(this);

		displayUtil.drawTextures(textures, 0, 0, zoom);

		displayController.updateDisplays(this);

		gl.glFlush();

		AWTGLReadBufferUtil rawScreenshot = new AWTGLReadBufferUtil(
				gl.getGLProfile(), true);
		screenBuffer = rawScreenshot.readPixelsToBufferedImage(gl, true);

		return screenBuffer;

	}
}
