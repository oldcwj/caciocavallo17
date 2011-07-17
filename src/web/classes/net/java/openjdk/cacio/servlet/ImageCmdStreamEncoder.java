package net.java.openjdk.cacio.servlet;

import java.awt.image.*;
import java.util.List;
import com.keypoint.*;
import net.java.openjdk.awt.peer.web.*;

public class ImageCmdStreamEncoder extends CmdStreamEncoder {

    protected void encodeImageCmdStream(BufferedImage bImg, List<Integer> cmdList) {
	bImg.setRGB(0, 0, cmdList.size());
	
	for(int i=0; i < cmdList.size(); i++) {
	    int pixelCnt = i+1;
	    int yPos = pixelCnt / bImg.getWidth();
	    int xPos = pixelCnt % bImg.getWidth();
	    
	    int intValue = cmdList.get(i);
	    int r = intValue < 0 ? 1 : 0; //sign
	    int gb = intValue & 0x0000FFFF;
	    
	    int rgb = r | gb;
	    bImg.setRGB(xPos, yPos, rgb);
	}
    }
    
    public byte[] getEncodedData(List<ScreenUpdate> pendingUpdateList, TreeImagePacker packer, List<Integer> cmdList) {
	DamageRect packedRegionBox = packer.getBoundingBox();
	int regionWidth = packedRegionBox.getWidth();
	int regionHeight = packedRegionBox.getHeight();
	int cmdAreaHeight = (int) Math.ceil(((double) cmdList.size() + 1) / (regionWidth));
	
	BufferedImage packedImage = new BufferedImage(regionWidth, regionHeight + cmdAreaHeight, BufferedImage.TYPE_INT_RGB);
	encodeImageCmdStream(packedImage, cmdList);
	copyUpdatesToPackedImage(pendingUpdateList, packedImage, cmdAreaHeight);
	
	return new PngEncoderB(packedImage, false, PngEncoder.FILTER_NONE, 2).pngEncode();
    }
    
}