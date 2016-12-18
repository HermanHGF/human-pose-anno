import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;

import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;


public class PoseAnno_BB_And_Joints extends JFrame implements DropTargetListener {

	private static final long serialVersionUID = 1L;

	// one bounding box, and 15 body joints	
	private static String[] annoName = {
		// a bounding box for a whole person
		"human_bb_top_left_cornor",
		"human_bb_bottom_right_cornor",
		// the body joints
		"head", "neck", "torso",
		"left_shoulder", "left_elbow", "left_hand/wrist",
		"right_shoulder", "right_elbow", "right_hand/wrist", 
		"left_hip", "left_knee", "left_foot/ankle",
		"right_hip", "right_knee", "right_foot/ankle"
	};
	
	// 
	private static int[] annoNameIndice = {
		0, 1, 2, 3, 4, 
		5, 6, 7, 8, 9, 10, 
		11, 12, 13, 14, 15
	};
	
	// if the occluded area of the body part is more that 30 percentage,
	// the body part is occluded
	private static String[] occState = { 
		"non_occ",	// default - 0
		"occ"		// 1 
	};
	
	private Image offScreenImage = null; // avoid flicker
	private Graphics gOffScreenGraphics;

	// handler of the annotated image
	private Image initImage = null;
	// path to the annotated dataset
	private String path = null;
	private String _prePath = null;
	private String _imagesetName = null;
	// the image name without extension
	private String fileName = null;
	private String fileName2 = null;
	private final String _labelExt = ".label";
	private final String _saveDirExt = "_AnnoPose";
	// image file list
	private LinkedList<File> file_list = new LinkedList<File>();
	// index of images
	private int curFileIdx = 0;

	// if shift is pressed, then draw
	// horizontal or
	// vertical line.
	private boolean shift_flag = false; 

	// flag to occlusion state
	// 0: non-occluded, 1: occluded
	private int[] isOccluded = new int[annoName.length];
	
	// each part is annotated by one point
	// private static final int numXY = 2;
	// private int[][] xPoints = new int[annoName.length + 1][numXY];
	// private int[][] yPoints = new int[annoName.length + 1][numXY];
	
	// the first two points for human bounding box
	// the rest for 15 human body joints
	private final int totalLen = annoName.length;
	private int[] xPoints = new int[totalLen];
	private int[] yPoints = new int[totalLen];
	// since human_bb occupies two points
	private int[] points = new int[totalLen];
	
	// index of parts
	private int partsIdx = 0;
	
	// initialize some variables
	private static final double scale = 1;
	private static final int _width = 640;
	private static final int _height = 480;
	private static final int _startX = 480;
	private static final int _startY = 240;
	
	private static final int _border = 10;
	private static final int _startJframeX = 0;
	private static final int _startJframeY = 20;
	private static final int _circle_len = 5;
	
	private static final String[] moustType = {
		"left mouse button", 
		"middle mouse wheel", 
		"right mouse button"
	};
	
	// ***************************************************************
	
	
	public PoseAnno_BB_And_Joints() {
		// 
		setTitle("Human Pose Annotation");
		setSize(_width, _height);
		setLocation(_startX, _startY);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// button 1: the left mouse button
		// button 2: the mouse wheel
		// button 3: the right mouse button
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int type = e.getButton();
				System.out.println("mouse click type: " + moustType[type - 1]);
				System.out.println("partsIdx: " + partsIdx);
				
				if (initImage == null || partsIdx == totalLen || type == MouseEvent.BUTTON2) {
					return;
				}

				if (type == MouseEvent.BUTTON3) {
					xPoints[partsIdx] = 0;
					yPoints[partsIdx] = 0;
					
					// need to be reset
					if (partsIdx < 0) {
						Arrays.fill(isOccluded, 0);
						Arrays.fill(xPoints, 0);
						Arrays.fill(yPoints, 0);
						
						partsIdx = 0;
					}
					
					PoseAnno_BB_And_Joints.this.repaint();
					return;
				}
				
				// get the point location
				xPoints[partsIdx] = e.getX() - _startJframeX;
				yPoints[partsIdx] = e.getY() - _startJframeY;
				
				// System.out.println("mouse click point: (" + 
				// 		e.getX() + ", " + e.getY() + ")");
				System.out.println("mouse click point: (" + xPoints[partsIdx] + 
						", " + yPoints[partsIdx] + ")");

				if (xPoints[partsIdx] < _border) {
					xPoints[partsIdx] = _border;
				}
				if (xPoints[partsIdx] >= initImage.getWidth(null) + _border) {
					xPoints[partsIdx] = initImage.getWidth(null) + _border - 1;
				}

				if (yPoints[partsIdx] < _border) {
					yPoints[partsIdx] = _border;
				}
				if (yPoints[partsIdx] >= initImage.getHeight(null) + _border) {
					yPoints[partsIdx] = initImage.getHeight(null) + _border - 1;
				}
				
				// isOccluded[partsIdx] = 0;
				// 
				partsIdx++;

				// repaint
				PoseAnno_BB_And_Joints.this.repaint();

				// 
				if (partsIdx == totalLen) {
					int choice = JOptionPane.showConfirmDialog(
							PoseAnno_BB_And_Joints.this,
							"complete! save annotation or not?", 
							"tips",
							JOptionPane.YES_NO_OPTION
					);
					if (choice == JOptionPane.YES_OPTION) {
						// save the annotation
						saveAnnotation();
						
						// set next image
						if (file_list.size() > 1) {
							setNextPic(1);
						}
					}
				}
			}
		});

		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				/*if (initImage == null || partsIdx == totalLen) {
					return;
				}*/
				
				// only for drawing a bounding box for the whole person
				// not for the 15 body joints
				if (initImage == null || partsIdx > 1) {
					return;
				}
				
				// System.out.println("mouse move");
				
				// get the point location -- keep lineIdx 
				xPoints[partsIdx] = e.getX() - _startJframeX;
				yPoints[partsIdx] = e.getY() - _startJframeY;
				
				// System.out.println("mouse move point: (" + e.getX() + 
				//			", " + e.getY() + ")");
				System.out.println("mouse click point: (" + xPoints[partsIdx] 
						+ ", " + yPoints[partsIdx] + ")");
				
				if (xPoints[partsIdx] < _border) {
					xPoints[partsIdx] = _border;
				}
				if (xPoints[partsIdx] >= initImage.getWidth(null) + _border) {
					xPoints[partsIdx] = initImage.getWidth(null) + _border - 1;
				}

				if (yPoints[partsIdx] < _border) {
					yPoints[partsIdx] = _border;
				}
				if (yPoints[partsIdx] >= initImage.getHeight(null) + _border) {
					yPoints[partsIdx] = initImage.getHeight(null) + _border - 1;
				}
				
				// repaint
				repaint();
			}
		});

		// o: 0: non-occluded, 1: occluded
		// u: back to pre joint
		// n: ahead to next-joint
		// r: reset the current image
		// s: save the current anno info
		// a: back to pre image
		// d: ahead to next image
		addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
			}

			// dose not modify
			@Override
			public void keyReleased(KeyEvent e) {
				switch (e.getKeyCode()) {
				case KeyEvent.VK_SHIFT:
					shift_flag = false;
					break;
				}
			}

			// need to be modified
			@Override
			public void keyPressed(KeyEvent e) {
				switch (Character.toLowerCase(e.getKeyChar())) {
				//  back to pre-part
				case 'u':
					if (partsIdx != 0) {
						System.out.println("u(back to the pre joint):" + " " + annoName[partsIdx]);
						
						// current part -- need to be reset
						xPoints[partsIdx] = 0;
						yPoints[partsIdx] = 0;
						isOccluded[partsIdx] = 0;	// set to be non-occlusion
						
						partsIdx--;
						
						// need to be reset
						if (partsIdx < 0) {
							Arrays.fill(isOccluded, 0);
							Arrays.fill(xPoints, 0);
							Arrays.fill(yPoints, 0);
							
							partsIdx = 0;
							return;
						}
						
						// current part -- need to be reset
						// really need?
						xPoints[partsIdx] = 0;
						yPoints[partsIdx] = 0;
						isOccluded[partsIdx] = 0;	// set to be non-occlusion
					}
					break;
					
				// save the annotation
				case 's':
					System.out.println("s(save)" + " " + file_list.get(curFileIdx));
					saveAnnotation();
					
					// set and init next image
					if (file_list.size() > 1) {
						setNextPic(1);
					}
					break;
					
				// ahead to next part
				case 'n':
					// skip current annotation due to missing
					if (partsIdx >= annoName.length)
						return;
					System.out.println("n(next joint): " + " " + annoName[partsIdx]);
					
					xPoints[partsIdx] = 0;
					yPoints[partsIdx] = 0;
					// set to be occlusion since you ignore the current part
					isOccluded[partsIdx] = 1;	
					
					partsIdx++;
					if (partsIdx == totalLen) {
						int choice = JOptionPane.showConfirmDialog(
								PoseAnno_BB_And_Joints.this,
								"complete! save annotation or not?", "tips",
								JOptionPane.YES_NO_OPTION
								);
						
						if (choice == JOptionPane.YES_OPTION) {
							// 
							saveAnnotation();
							
							// set and init next image
							// if (file_list.size() > 1) {
								// setNextPic(1);
							// }
						}
					}
					break;

				// back to pre image
				case 'a':
					// back to pre image and init pre image status
					System.out.println("a(back from):" + " " + file_list.get(curFileIdx));
					
					// remove the current label file, if it exist
					String annoPath = _prePath + "/" + _imagesetName + _saveDirExt;
					File file = new File(annoPath);
					file.mkdir();
					String annoLabelFile = annoPath + "/" + fileName + _labelExt;
					file = new File(annoLabelFile);
					if(file.exists()) {
						file.delete();
					}
					
					// then back to pre image
					setNextPic(0);
					break;
				
				// ahead to next image
				case 'd':
					// set and init next image status
					// saveAnnotation();   
					// just ignore the current image
					System.out.println("d(ahead to):" + " " + file_list.get(curFileIdx));
					setNextPic(1);
					break;

				// reset the current image
				case 'r':
					System.out.println("r(rest):" + " " + file_list.get(curFileIdx));
					
					partsIdx = 0;
					Arrays.fill(xPoints, 0);
					Arrays.fill(yPoints, 0);
					Arrays.fill(isOccluded, 0);
					break;
				
				// 1: partial occluded
				case 'o':
					System.out.println("if the part is occluded, you should " +
							"press the 'o' key before click the mouse");
					System.out.println("o(occ): " + " " + annoName[partsIdx]);
					isOccluded[partsIdx] = 1;
					break;
					
				default:
					switch (e.getKeyCode()) {
					case KeyEvent.VK_ESCAPE:
						xPoints[partsIdx] = 0;
						yPoints[partsIdx] = 0;
						isOccluded[partsIdx] = 1;	// set to be non-occlusion
						break;

					case KeyEvent.VK_SHIFT:
						shift_flag = true;
						break;
					}
				}
				
				// repaint
				PoseAnno_BB_And_Joints.this.repaint();
			}
		});

		new DropTarget(this, this);

		setVisible(true);
	}
	
	protected void saveAnnotation() {
		saveAnnoPose();
	}
	
	protected void saveAnnoPose() {
		try {
			String annoPath = _prePath + "/" + _imagesetName + _saveDirExt;
			File file = new File(annoPath);
			file.mkdir();
			String annoLabelFile = annoPath + "/" + fileName + _labelExt;
			file = new File(annoLabelFile);
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			// the total path to the image
			String str = fileName2 + "\n";
			double x1, y1;
			
			// body joints
			for (int i = 0; i < totalLen - 1; i++) {
				x1 = Math.max( (xPoints[i] - _border) / scale, 0 );	
				y1 = Math.max( (yPoints[i] - _border) / scale, 0 );
				str = str + annoName[i] + " " + x1 + " " + y1 + " " 
						+ isOccluded[i] + "\n";
			}
			
			x1 = Math.max( (xPoints[totalLen - 1] - _border) / scale, 0 );	
			y1 = Math.max( (yPoints[totalLen - 1] - _border) / scale, 0 );
			str = str + annoName[totalLen - 1] + " " + x1 + " " + y1 + " " 
					+ isOccluded[totalLen - 1];
			str = str + "\n";
			
			bw.write(str);
			bw.close();

		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "failed to save annotation");
		}
	}

	@Override
	public void update(Graphics g) {
		if (initImage == null)
			return;

		if (offScreenImage == null
				|| offScreenImage.getHeight(this) != this.getHeight()
				|| offScreenImage.getWidth(this) != this.getWidth() ) {
			
			offScreenImage = this.createImage(this.getWidth(), this.getHeight());
			
			gOffScreenGraphics = offScreenImage.getGraphics();
		}
		
		int imgWidth = initImage.getWidth(null);
		int imgHeigth = initImage.getHeight(null);
		
		int osImgWidth = offScreenImage.getWidth(null);
		int osImgHeight = offScreenImage.getHeight(null);
		
		System.out.println("imgWidth: " + imgWidth + ", imgHeight: " + imgHeigth + 
				", oImgWidth: " + osImgWidth + ", oImgHeight: " + osImgHeight);

		// a top-left corner (_border, _border)
		gOffScreenGraphics.drawImage(initImage, _border, _border, this); 
		
		((Graphics2D) gOffScreenGraphics).setStroke(new BasicStroke(1.50f));
		gOffScreenGraphics.setColor(Color.GREEN);
		
		if (partsIdx < totalLen) {
			gOffScreenGraphics.drawString("" + partsIdx + ": " + annoName[partsIdx], 20, 20);
		} else {
			gOffScreenGraphics.drawString("" + partsIdx + ": " + 
					"annotatation has been done!", 20, 50);
		}
		
		gOffScreenGraphics.setColor(Color.ORANGE);
		// draw the current line
		int i;
		// draw the joints
		for(i = 0; i < partsIdx; i++) {
			gOffScreenGraphics.fillOval(xPoints[i], yPoints[i], _circle_len, _circle_len);
		}
		
		// draw the bounding box for human
		gOffScreenGraphics.setColor(Color.RED);
		if(partsIdx > 1) {
			int lineX[] = {xPoints[0], xPoints[0], xPoints[1], xPoints[1], xPoints[0]};
			int lineY[] = {yPoints[0], yPoints[1], yPoints[1], yPoints[0], yPoints[0]};
			for (i = 0; i < lineX.length - 1; i++) {
				gOffScreenGraphics.drawLine(lineX[i], lineY[i], lineX[i + 1], lineY[i + 1]);
			}
		}
				
		// paint(gOffScreen);
		g.drawImage(offScreenImage, _startJframeX, _startJframeY, this);
		g.dispose();
	}

	@Override
	public void paint(Graphics g) {
		update(g);
	}

	// *********************************************
	@Override
	public void dragEnter(DropTargetDragEvent dtde) {
	
	}

	@Override
	public void dragOver(DropTargetDragEvent dtde) {
	
	}

	@Override
	public void dropActionChanged(DropTargetDragEvent dtde) {
	
	}

	@Override
	public void dragExit(DropTargetEvent dte) {
	
	}
	// *******************************************
	
	// drop the images into UI
	// get the image file list
	@SuppressWarnings("rawtypes")
	@Override
	public void drop(DropTargetDropEvent dtde) {
		try {
			if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
				dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
				
				List list = (List) (dtde.getTransferable()
						.getTransferData(DataFlavor.javaFileListFlavor));
				
				Iterator iterator = list.iterator();
				file_list.clear();
				curFileIdx = -1;
				
				while (iterator.hasNext()) {
					File file = (File) iterator.next();
					if (file.isDirectory()) {
						addFileToQueue(file);
					} else {
						if (file.getName().endsWith(".jpg") || 
								file.getName().endsWith(".png"))
							file_list.add(file);
					}
				}
				
				// start & init
				setNextPic(1);
				dtde.dropComplete(true);
			} else {
				dtde.rejectDrop();
			}
		} catch (Exception e) {
			e.printStackTrace();
			dtde.rejectDrop();
			JOptionPane.showMessageDialog(this, "Failed to open images");
		}
	}
		
	// flag == 0 -- previous pic
	// flag == 1 -- next pic
	private void setNextPic(int flag) {
		int tmp = curFileIdx;

		// back
		if (flag == 0) {
			if (curFileIdx == 0) {
				JOptionPane.showMessageDialog(this, "The first one!");
				return;
			}
			curFileIdx--;
		} else {
			// next
			curFileIdx++;
		}
		
		boolean ok = false;
		while (!ok) {
			try {
				if (curFileIdx >= file_list.size()) {
					curFileIdx = tmp;
					if (curFileIdx == -1)
						return;
					JOptionPane.showMessageDialog(this, "The last one!");
					return;
				}
				File file = file_list.get(curFileIdx);
				
				initImage = ImageIO.read(file);

				initImage = initImage.getScaledInstance(
					(int) (initImage.getWidth(null) * scale),
					(int) (initImage.getHeight(null) * scale),
					Image.SCALE_SMOOTH	// SCALE_DEFAULT, SCALE_SMOOTH
				);		

				// path and fileName
				int idx;
				path = file.getParent();
				String fileSeparator = System.getProperty("file.separator");
				// idx = path.lastIndexOf('\\');	// here is '\'
				idx = path.lastIndexOf(fileSeparator);	// here is '\'
				System.out.println("idx " + idx);
				System.out.println("fileSeparator " + fileSeparator);
				System.out.println("path " + path);
				if(idx != 0){
					_prePath = path.substring(0, idx);
					_imagesetName = path.substring(idx + 1);
				}
				
				int imgLen = file_list.size();
				fileName = file.getName();
				fileName2 = path + fileSeparator + file.getName();
				idx = fileName.lastIndexOf('.');
				if (idx != -1) {
					fileName = fileName.substring(0, idx);
				}
				setTitle(fileName + " in " + imgLen + " imageset");

				// 
				partsIdx = 0;
				
				// set default value - 0: non_occ, 1: occ
				Arrays.fill(isOccluded, 0);
				// 1 bb for person and 15 joints
				Arrays.fill(points, 0);
				
				Arrays.fill(xPoints, 0);
				Arrays.fill(yPoints, 0);
				
				// _border: 10
				// _startJframeX: 0
				// _startJframeY: 20
				setSize(initImage.getWidth(null)  + _border * 3 + _startJframeX,
					initImage.getHeight(null) + _border * 3 + _startJframeY
				);

				// readAnnotation(path + "/Annotations/" + fileName + ".xml");
				repaint();

				ok = true;

			} catch (Exception e) {
				e.printStackTrace();
				if (flag == 0) {
					if (curFileIdx == 0) {
						return;
					}
					curFileIdx--;
				} else {
					curFileIdx++;
				}
			}
		}
	}
	
	// add file to file_list
	// if meet folder then recur it
	private void addFileToQueue(File folder) {
		for (File file : folder.listFiles()) {
			if (file.isDirectory()) {
				addFileToQueue(file);
			} else {
				if (file.getName().endsWith(".jpg") || file.getName().endsWith(".png")) {
					file_list.add(file);
				}
			}
		}
	}
	
	// here we don't use it
	private boolean readAnnotation(String path) {
		File gt = new File(path);
		if (!gt.exists())
			return false;

		int choice = JOptionPane.showConfirmDialog(this,
				"Annotation file is found, read it or not?", "tips",
				JOptionPane.YES_NO_OPTION);
		if (choice != JOptionPane.YES_OPTION)
			return false;

		try {
			// ??
		} catch (Exception e) {
			e.printStackTrace();
			partsIdx = 0;
			Arrays.fill(xPoints, 0);
			Arrays.fill(yPoints, 0);
			Arrays.fill(isOccluded, 0);
			JOptionPane.showMessageDialog(this, "Invalid annotation file");
		}
		return true;
	}

	
	// **********************************************
	
	
	public static void main(String[] args) {
		new PoseAnno_BB_And_Joints();
	}

}
