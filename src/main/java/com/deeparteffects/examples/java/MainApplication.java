package com.deeparteffects.examples.java;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.Timer;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.opensdk.config.ConnectionConfiguration;
import com.amazonaws.opensdk.config.TimeoutConfiguration;
import com.deeparteffects.examples.java.CheckResultTask.CheckResultListener;
import com.deeparteffects.sdk.java.DeepArtEffectsClient;
import com.deeparteffects.sdk.java.model.GetStylesRequest;
import com.deeparteffects.sdk.java.model.GetStylesResult;
import com.deeparteffects.sdk.java.model.PostUploadRequest;
import com.deeparteffects.sdk.java.model.PostUploadResult;
import com.deeparteffects.sdk.java.model.Style;
import com.deeparteffects.sdk.java.model.UploadRequest;

@SuppressWarnings({"unchecked", "rawtypes"})
public class MainApplication extends JFrame {
	
	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(MainApplication.class);

	private static final String API_KEY = "--INSERT YOUR API KEY--";
	private static final String ACCESS_KEY = "--INSERT YOUR ACCESS KEY--";
	private static final String SECRET_KEY = "--INSERT YOUR SECRET KEY--";
	
	private static final int CHECK_RESULT_INTERVAL_IN_MS = 2500;
	private static final int MAX_IMAGE_SIZE = 1980;

	private DeepArtEffectsClient deepArtEffectsClient;
	private boolean isProcessing = false;
	private String imageBase64Encoded;
	private List<Style> styles;

	private JPanel contentPane;
	private JLabel lblNewLabel;
	private JList list;
	private JScrollPane scrollPane;
	private JLabel lblChooseAStyle;
	private JPanel imagePanel;
	private JProgressBar progressBar;

	public static void main(String[] args) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException,
			UnsupportedLookAndFeelException {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					new MainApplication();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public MainApplication() {
		setTitle("Deep Art Effects");
		setMinimumSize(new Dimension(640, 480));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setMinimumSize(new Dimension(640, 480));
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		imagePanel = new JPanel();
		imagePanel.setBorder(null);
		imagePanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {

				JFileChooser chooser = new JFileChooser();
				FileFilter imageFilter = new FileNameExtensionFilter("Image files", ImageIO.getReaderFileSuffixes());
				chooser.setFileFilter(imageFilter);
				chooser.setAcceptAllFileFilterUsed(false);
				int returnVal = chooser.showOpenDialog(MainApplication.this);

				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = chooser.getSelectedFile();
					try {
						BufferedImage image = ImageIO.read(file);
						if (image != null) {
							BufferedImage thumbnail = scaleImage(image, imagePanel.getWidth(), imagePanel.getHeight());
							if (thumbnail != null) {
								lblNewLabel.setIcon(new ImageIcon(thumbnail));
								lblNewLabel.setText(null);
							}
							BufferedImage photo = image;
							if(image.getWidth()>MAX_IMAGE_SIZE | image.getHeight() > MAX_IMAGE_SIZE) {
								photo = scaleImage(image, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE);
							} 
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							ImageIO.write(photo, "jpg", baos);
							byte[] bytes = baos.toByteArray();
							imageBase64Encoded = Base64.getEncoder().encodeToString(bytes);
						}
					} catch (IOException ex) {
						// TODO Auto-generated catch block
						ex.printStackTrace();
					}

					logger.info("Choosing file " + file.getPath());
				} else {
					logger.info("Open command cancelled by user.");
				}
			}
		});
		GridBagLayout gbl_imagePanel = new GridBagLayout();
		gbl_imagePanel.columnWidths = new int[] { 153, 0 };
		gbl_imagePanel.rowHeights = new int[] { 197, 0 };
		gbl_imagePanel.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_imagePanel.rowWeights = new double[] { 1.0, Double.MIN_VALUE };
		imagePanel.setLayout(gbl_imagePanel);

		contentPane.add(imagePanel, BorderLayout.CENTER);
		lblNewLabel = new JLabel("Click here to load image");
		lblNewLabel.setHorizontalTextPosition(SwingConstants.LEADING);
		lblNewLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.anchor = GridBagConstraints.CENTER;
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		imagePanel.add(lblNewLabel, gbc_lblNewLabel);

		list = new JList();
		list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		list.setVisibleRowCount(1);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setCellRenderer(new ImageListCellRenderer());
		list.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if(imageBase64Encoded==null) {
					JOptionPane.showMessageDialog(MainApplication.this, "Please choose a picture first.");
					return;
				}
				
				if (!isProcessing) {
					isProcessing = true;
					progressBar.setVisible(true);
					int index = list.getSelectedIndex();
					Style style = styles.get(index);
					String submissionId = uploadImage(style.getId());
					checkResult(submissionId);
				}
			}
		});

		scrollPane = new JScrollPane();
		scrollPane.setPreferredSize(new Dimension(640, 150));
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		scrollPane.setViewportView(list);
		contentPane.add(scrollPane, BorderLayout.SOUTH);

		lblChooseAStyle = new JLabel("Choose a style");
		scrollPane.setColumnHeaderView(lblChooseAStyle);

		progressBar = new JProgressBar();
		progressBar.setIndeterminate(true);
		progressBar.setVisible(false);
		contentPane.add(progressBar, BorderLayout.NORTH);

		SplashScreen splashscreen = new SplashScreen();

		// Initialise Deep Art Effects Client
		BasicAWSCredentials credentials = new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY);
		deepArtEffectsClient = DeepArtEffectsClient
				.builder()
				.connectionConfiguration(
						new ConnectionConfiguration().maxConnections(100)
								.connectionMaxIdleMillis(1000))
				.timeoutConfiguration(
						new TimeoutConfiguration().httpRequestTimeout(3000)
								.totalExecutionTimeout(10000)
								.socketTimeout(2000))
				.apiKey(API_KEY)
				.iamCredentials(new AWSStaticCredentialsProvider(credentials))
				.build();

		new Thread(new Runnable() {
			GetStylesResult getStylesResult = deepArtEffectsClient.getStyles(new GetStylesRequest());

			@Override
			public void run() {
				styles = getStylesResult.getStyles();
				updateUI(styles);
				splashscreen.dispose();
				setVisible(true);
			}
		}).start();
	}

	private BufferedImage scaleImage(BufferedImage image, int width, int height) {
		BufferedImage thumbnail = null;
		try {
			thumbnail = Thumbnails.of(image)
					.size(width, height)
					.keepAspectRatio(true).asBufferedImage();
			return thumbnail;
		} catch (IOException e) {
			return null;
		}
	}

	private String uploadImage(int styleId) {
		logger.info(String.format("Render image with style %d", styleId));
		PostUploadRequest postUploadRequest = new PostUploadRequest();
		UploadRequest uploadRequest = new UploadRequest();
		uploadRequest.setStyleId(styleId);
		uploadRequest.setImageBase64Encoded(imageBase64Encoded);
		//uploadRequest.setImageSize(MAX_IMAGE_SIZE);
		postUploadRequest.setUploadRequest(uploadRequest);
		PostUploadResult postUploadResult = deepArtEffectsClient.postUpload(postUploadRequest);
		return postUploadResult.getUploadResponse().getSubmissionId();
	}

	private void checkResult(String submissionId) {
		logger.info(String.format("Processing image with submissionId %s", submissionId));
		Timer timer = new Timer();
		CheckResultTask task = new CheckResultTask(deepArtEffectsClient,
				submissionId, new CheckResultListener() {
					@Override
					public void onFinish(String url) {
						logger.info(String.format("Finished processing"));
						try {
							InputStream stream = new URL(url).openStream();
							ImageInputStream iis;
							iis = ImageIO.createImageInputStream(stream);
							BufferedImage image = ImageIO.read(iis);
							if (image != null) {
								BufferedImage thumbnail = scaleImage(image, imagePanel.getWidth(), imagePanel.getHeight());
								if (thumbnail != null) {
									lblNewLabel.setIcon(new ImageIcon(thumbnail));
									lblNewLabel.setText(null);
								}
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} finally {
							isProcessing = false;
							progressBar.setVisible(false);
						}
					}

					@Override
					public void onError() {
						logger.info(String
								.format("Error occured while processing image"));
						isProcessing = false;
						progressBar.setVisible(false);
					}
				});
		timer.schedule(task, CHECK_RESULT_INTERVAL_IN_MS, CHECK_RESULT_INTERVAL_IN_MS);
	}

	private void updateUI(List<Style> styles) {
		Vector<JPanel> panels = new Vector<JPanel>();
		for (Style style : styles) {
			BufferedImage image = null;
			try {
				InputStream stream = new URL(style.getUrl()).openStream();
				ImageInputStream iis = ImageIO.createImageInputStream(stream);
				image = ImageIO.read(iis);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			JPanel panel = new JPanel();
			JLabel label = new JLabel();
			label.setSize(new Dimension(150, 150));
			if (image != null) {
				BufferedImage thumbnail;
				try {
					thumbnail = Thumbnails.of(image)
							.size(100, 100)
							.crop(Positions.CENTER)
							.asBufferedImage();
					label.setIcon(new ImageIcon(thumbnail));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				panel.add(label);
				panels.addElement(panel);
			}
		}
		list.setListData(panels);
		list.invalidate();
		scrollPane.invalidate();
	}

	class ImageListCellRenderer implements ListCellRenderer<Object> {
		public Component getListCellRendererComponent(JList jlist,
				Object value, int cellIndex, boolean isSelected,
				boolean cellHasFocus) {
			Component component = (Component) value;
			component.setForeground(Color.white);
			component.setBackground(isSelected ? UIManager
					.getColor("Table.focusCellForeground") : Color.white);
			return component;
		}
	}
}
