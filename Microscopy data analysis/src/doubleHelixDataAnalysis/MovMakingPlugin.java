package doubleHelixDataAnalysis;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;

import miatool.core.setssingles.image.ImageSet;
import miatool.core.setssingles.label.LabelSet;
import miatool.core.setssingles.label.LabelSingle;
import miatool.core.setssingles.label.TextLabel;
import miatool.core.setssingles.massage.MassageSet;
import miatool.miamain.MIABrowser;
import miatool.miamain.MIATool;
import miatool.miamain.MIAToolMain;
import miatool.miamain.miatoolhub.MIAToolHub;
import miatool.plugins.MIAPlugin;
import miatool.plugins.PluginUtil;
import miatool.tools.MIAToolDirectoryBrowser;
import miatool.tools.adjustmenttools.labeltool.LabelTool;
import miatool.tools.displaytool.ImageSetBrowser;
import miatool.tools.viewer.OffscreenController;
import miatool.ui.components.slider.MIAIndicesScroller;
import miatool.ui.components.slider.MassagedIndicesScroller;
import miatool.ui.components.textfield.DoubleTextField;
import miatool.ui.components.textfield.IntegerTextField;
import miatool.ui.events.MIAPropertyChangeEvent;
import miatool.ui.events.MIAValueChangeEvent;
import miatool.ui.events.MIAValueChangeListener;
import net.miginfocom.swing.MigLayout;

/**
 * Provides a user with the ability to make movies for different sliders from
 * images loaded to miatool image viewer. Also contains some features, e.g.
 * frame rate and compression, to control the type of the output movie
 * 
 * @since 0.1
 * @version 0.1
 * @author Milad Rafiee Vahid
 */
public class MovMakingPlugin extends JPanel implements
		ActionListener, MIAPlugin, ChangeListener, MIAValueChangeListener {

	private static final long serialVersionUID = 1L;

	/** Global and constant variables */
	private static int moveSlider;
	/** GUI Elements */
	private JSpinner jspinner = new JSpinner();
	private final IntegerTextField tfStartFrame = new IntegerTextField();
	private final DoubleTextField tfScalingFactor = new DoubleTextField();
	private final IntegerTextField tfEndFrame = new IntegerTextField();
	private final IntegerTextField tfFrameRate = new IntegerTextField();
	private final JLabel lblSliderToMove = new JLabel("Slider to move");
	private final JLabel lblStartFrame = new JLabel("Start Frame");
	private final JLabel lblStepSize = new JLabel("Scaling Factor");
	private final JLabel lblEndFrame = new JLabel("End Frame");
	private final JLabel lblFramerate = new JLabel("FrameRate");
	private final JLabel lblFps = new JLabel("Fps");
	private final JPanel playbackPnl = new JPanel();
	private final JPanel mainPnl = new JPanel();
	private final JButton btnMakeMovie = new JButton("Make Movie");
	private final JPanel annotationPanel = new JPanel();
	private final JLabel lblExposureTime = new JLabel("Exposure Time");
	private final DoubleTextField tfExposureTime = new DoubleTextField();
	private final JButton btnCreateExposureTime = new JButton(
			"Create Exposure Time Label Set");

	private final JFrame frame = new JFrame("Movie Maker Tool");
	private final JMenuItem pluginMnuItm = new JMenuItem("Movie Maker");

	/** Initializing GUI components. */
	int startFrame;
	int endFrame;
	double scalingFactor;
	int frameRate;
	File file;

	/** Draws the components of the different panels. */
	public MovMakingPlugin() {
		tfExposureTime.setColumns(10);
		drawMainpnl();
		drawPlayBackpnl();
		drawRenderOptionspnl();
		wireListeners();

		frame.getContentPane().setLayout(new BorderLayout());
		frame.setBounds(0, 0, 100, 100);
		frame.getContentPane().add(this, BorderLayout.CENTER);
		frame.pack();
		frame.setLocationRelativeTo(null);
	}

	/**
	 * Convert the type of the input BufferedImage to the desired type
	 * 
	 * @param sourceImage
	 *        Input BufferedImage
	 * 
	 * @param targetType
	 *        The integer indicating the desired BufferedImage type
	 * 
	 * @return The BufferedImage containing the converted image to the desired
	 *         targetType
	 * 
	 */
	private static BufferedImage convertToType(
			final BufferedImage sourceImage,
			final int targetType) {

		BufferedImage image;

		// If the source image is already the target type, return the source
		// image
		if (sourceImage.getType() == targetType) {
			image = sourceImage;
		}

		else {
			image = new BufferedImage(
					sourceImage.getWidth(),
					sourceImage.getHeight(),
					targetType);

			image.getGraphics().drawImage(sourceImage, 0, 0, null);
		}

		return image;
	}

	public static void main(String [] args) {
		MIATool.main(new String [0]);
		MovMakingPlugin panel = new MovMakingPlugin();
		PluginUtil.addPlugin(panel);
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		if (e.getSource() == btnMakeMovie) {
			final String path;
			path = MIATool.getMIAToolHub().getMIAToolDirectory().getRootPath();
			final JFileChooser jFC = new JFileChooser(path);

			jFC.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			jFC.setMultiSelectionEnabled(false);

			final int result = jFC.showOpenDialog(frame);
			if (result == JFileChooser.APPROVE_OPTION) {
				file = jFC.getSelectedFile();
			}

			movieMaking();
		}
		else if (e.getSource() == btnCreateExposureTime) {
			createExposureTimeLabels();
		}
		else if (e.getSource() == pluginMnuItm) {
			frame.setVisible(true);
		}

	}

	private void createExposureTimeLabels() {
		double expTime = tfExposureTime.getValue();
		if (expTime <= 0) {
			return;
		}
		MIAToolHub mthub = MIATool.getMIAToolHub();

		if (mthub == null) {
			return;
		}
		MIAToolMain mtm = mthub.getMIAToolMain();
		if (mtm == null) {
			return;
		}
		ImageSetBrowser imgb = mtm.getImageSetBrowser();
		ImageSet imgSet = imgb.getActiveObject();

		if (imgSet == null) {
			return;
		}
		LabelSet labSet = new LabelSet(imgSet);

		int [] indices = mthub.getIndicesScroller().getCurrentIndex1i();
		int endFrame = tfEndFrame.getValue();
		int startFrame = tfStartFrame.getValue();
		// double scalingFactor = tfScalingFactor.getValue();

		for (int i = 0; i <= (endFrame - startFrame); i++) {

			int index = startFrame + i;

			indices[moveSlider] = index;
			LabelSingle labSingle = new LabelSingle();
			labSingle.add(new TextLabel(index * expTime + "s", false, 0, 0));

			labSet.set1iR(labSingle, indices);
		}

		labSet.setParent(imgSet);
		labSet.setFilename("ExposureTimes");
		try {
			imgSet.getParent().saveObject(labSet);
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		LabelTool ltool = mthub.getTool(LabelTool.class, LabelSet.class);
		ltool.updateFields();
		ltool.getSetBrowser().setAsLoaded(labSet);
	}

	@Override
	public void dispose() {}

	/** Draws all components of the Main panel. */
	private void drawMainpnl() {
		setLayout(new BorderLayout(0, 0));
		mainPnl.setLayout(new MigLayout("", "[grow,fill][132.00,grow,fill]",
				"[fill][grow]"));
		annotationPanel.setBorder(new TitledBorder(null, "Annotation Options",
				TitledBorder.LEADING, TitledBorder.TOP, null, null));
		annotationPanel.setLayout(new MigLayout("", "[][grow]", "[][]"));
		annotationPanel.add(lblExposureTime, "cell 0 0,alignx trailing");
		annotationPanel.add(tfExposureTime, "cell 1 0,growx");
		annotationPanel.add(btnCreateExposureTime, "cell 0 1 2 1");

		mainPnl.add(annotationPanel, "cell 0 1,grow");

		mainPnl.add(btnMakeMovie, "cell 1 1,alignx right");
		add(mainPnl);
	}

	/** Draws all components of the Play back panel. */
	private void drawPlayBackpnl() {
		playbackPnl.setBorder(new TitledBorder(
				null,
				"Playback Options",
				TitledBorder.LEADING,
				TitledBorder.TOP, null, null));

		playbackPnl.setLayout(new MigLayout("", "[left][grow]", "[][][][][]"));

		final SpinnerModel model = new SpinnerNumberModel(1, 1, 3, 1);
		jspinner = new JSpinner(model);
		playbackPnl.add(lblSliderToMove, "cell 0 0");
		playbackPnl.add(jspinner, "cell 1 0");

		playbackPnl.add(lblStartFrame, "cell 0 1");
		playbackPnl.add(tfStartFrame, "cell 1 1,growx");

		playbackPnl.add(lblStepSize, "cell 0 2");
		playbackPnl.add(tfScalingFactor, "cell 1 2,growx");

		playbackPnl.add(lblEndFrame, "cell 0 3");
		playbackPnl.add(tfEndFrame, "cell 1 3,growx");

		playbackPnl.add(tfFrameRate, "flowx,cell 1 4");
		playbackPnl.add(lblFramerate, "cell 0 4");

		playbackPnl.add(lblFps, "cell 1 4");

		mainPnl.add(playbackPnl, "cell 0 0");
	}

	/** Draws all components of the Render Options panel. */
	private void drawRenderOptionspnl() {}

	// Initializer
	@Override
	public JMenuItem getMenuItem() {
		return pluginMnuItm;
	}

	/**
	 * Creates a movie from a set of Buffered images exported from the image
	 * viewer in which we grab each frame from the image viewer and right after
	 * add this to the movie )
	 */

	private void movieMaking() {
		List<BufferedImage> imgs = new ArrayList<>();
		MassagedIndicesScroller slider = MIATool.getMIAToolHub()
				.getIndicesScroller();
		int [] index = slider.getCurrentIndex1i();
		MassagedIndicesScroller mis;

		MassageSet massSet = slider.getMassageSet();

		if (massSet != null) {
			mis = new MassagedIndicesScroller(massSet);
		}
		else {
			mis = new MassagedIndicesScroller(slider.getMaxima1i());
		}
		mis.setCurrentIndex1i(index);

		OffscreenController oControl = new OffscreenController(mis);
		oControl.setScalingFactor(0, scalingFactor);

		final IMediaWriter writer = ToolFactory
				.makeWriter(file.getAbsolutePath() + ".mp4");

		BufferedImage rendered = oControl.getScreenshot(0);
		int width = rendered.getWidth();
		int height = rendered.getHeight();
		writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_H264, width, height);
		for (int i = startFrame; i <= endFrame; i += 1) {
			index[moveSlider] = i;
			mis.setCurrentIndex1i(index);
			rendered = oControl.getScreenshot(0);
			BufferedImage bgrScreen = convertToType(rendered,
					BufferedImage.TYPE_3BYTE_BGR);
			writer.encodeVideo(0, bgrScreen,
					1000000000L * (i - startFrame + 1) / frameRate,
					TimeUnit.NANOSECONDS);
		}
		writer.close();

		System.out.println("Done");

	}

	@Override
	public Object [] pluginCommand(String cmd, Object... input) {
		return null;
	}

	@Override
	public void processPropertyChange(final MIAPropertyChangeEvent<?> e)
			throws IllegalArgumentException {

		final Object changedProp = e.getChangedProperty();
		if (changedProp == MIAToolDirectoryBrowser.Property.MIATOOLDIRECTORY) {
			setDefaultValue();
		}
		else if (changedProp == MIAIndicesScroller.Property.LOCKED_DIMENSION) {
			setDefaultValue();
		}
		else if (changedProp == MIABrowser.Property.UNLOADED) {
			setDefaultValue();
		}
	}

	@Override
	public void processValueChange(MIAValueChangeEvent e) {
		Object src = e.getSource();

		if (tfEndFrame == src) {
			endFrame = tfEndFrame.getValue();
		}

		if (tfStartFrame == src) {
			startFrame = tfStartFrame.getValue();
		}

		if (tfScalingFactor == src) {
			scalingFactor = tfScalingFactor.getValue();
		}

		if (tfFrameRate == src) {
			frameRate = tfFrameRate.getValue();
		}

	}

	@Override
	public void saveConfigToPrefs() {}

	@Override
	public void saveConfigToSettings() {}

	@Override
	public void setConfigFromPrefs() {}

	@Override
	public void setConfigFromSettings() {}

	/**
	 * In the case of the user dosen't define the output parameters, sets the
	 * default value for output parameters based on which slider chosen by the
	 * user.
	 */
	private void setDefaultValue() {
		MassagedIndicesScroller slider = MIATool.getMIAToolHub()
				.getIndicesScroller();
		final int [] maximum = slider.getCompatibleSizes();
		tfEndFrame.setValue(maximum[moveSlider]);
		final int [] index = slider.getCurrentIndex1i();
		tfStartFrame.setValue(index[moveSlider]);
		tfScalingFactor.setValue((double) 1);
		tfFrameRate.setValue(10);
	}

	@Override
	public void setVisible(final boolean b) {
		// TODO Auto-generated method stub

	}

	@Override
	public void startup() {
		MIATool.getMIAToolHub()
				.getIndicesScroller()
				.addMIAPropertyChangeListener(this);
		MIATool.getMIAToolHub()
				.getMIAToolMain()
				.getImageSetBrowser()
				.addMIAPropertyChangeListener(this);
	}

	@Override
	public void stateChanged(final ChangeEvent e) {
		final Object src = e.getSource();
		if (src == jspinner) {
			try {
				jspinner.commitEdit();
				moveSlider = (Integer) jspinner.getValue() - 1;
				setDefaultValue();
			}
			catch (final ParseException e1) {
				((DefaultEditor) jspinner.getEditor()).getTextField().setText(
						jspinner.getValue().toString());
			}
		}
	}

	@Override
	public void updateFields() {}

	public void wireListeners() {
		jspinner.addChangeListener(this);
		btnMakeMovie.addActionListener(this);
		btnCreateExposureTime.addActionListener(this);
		pluginMnuItm.addActionListener(this);
		tfEndFrame.addValueChangeListener(this);
		tfStartFrame.addValueChangeListener(this);
		tfFrameRate.addValueChangeListener(this);
		tfExposureTime.addValueChangeListener(this);
		tfScalingFactor.addValueChangeListener(this);
	}

}

/**
 * REVISION HISTORY:
 *
 * 2015-04-24, 0.1, Milad Rafiee: First ongoing draft.
 *
 */
