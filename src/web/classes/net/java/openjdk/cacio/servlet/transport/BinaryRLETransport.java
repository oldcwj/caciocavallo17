package net.java.openjdk.cacio.servlet.transport;

import java.awt.image.*;
import java.io.*;
import java.util.*;

import net.java.openjdk.awt.peer.web.*;
import net.java.openjdk.cacio.servlet.imgformat.*;

public class BinaryRLETransport extends BinaryTransport {

    RLEImageEncoder rleEncoder;

    public BinaryRLETransport() {
	rleEncoder = new RLEImageEncoder();
    }

    @Override
    public void writeEncodedData(OutputStream os, List<ScreenUpdate> pendingUpdateList, TreeImagePacker packer, List<Integer> cmdList)
	    throws IOException {
	WebRect packedRegionBox = packer.getBoundingBox(); //Handle case whrer width|height = 0

	byte[] cmdStreamData = encodeImageCmdStream(cmdList);
	os.write(cmdStreamData);
	
	if(packedRegionBox.getWidth() == 0 || packedRegionBox.getHeight() == 0) {
	    return;
	}

	// Fast-Path: If there is only a single BlitScreenUpdate, encode
	// directly from the SurfaceData and avoid an additional blit
	BlitScreenUpdate singleUpdate = getLonelyBlitScreenUpdate(pendingUpdateList);
	if (singleUpdate != null) {
	    WebRect updateArea = singleUpdate.getUpdateArea();
	    rleEncoder.encodeImageToStream(singleUpdate.getImage(), singleUpdate.getSrcX(), singleUpdate.getSrcY(), singleUpdate.getSrcX()
		    + updateArea.getWidth(), singleUpdate.getSrcY() + updateArea.getHeight(), os);
	} else {
	    BufferedImage packedImage = new BufferedImage(packedRegionBox.getWidth(), packedRegionBox.getHeight(), BufferedImage.TYPE_INT_RGB);
	    copyUpdatesToPackedImage(pendingUpdateList, packedImage, 0);
	    rleEncoder.encodeImageToStream(packedImage, 0, 0, packedImage.getWidth(), packedImage.getHeight(), os);
	}
    }

    /**
     * Returns the one and only BlitScreenUpdate of a list, or null if the List
     * has more than one BlitScreenCommands
     * 
     * @param updateList
     * @return
     */
    protected BlitScreenUpdate getLonelyBlitScreenUpdate(List<ScreenUpdate> updateList) {
	BlitScreenUpdate lonelyUpdate = null;

	for (ScreenUpdate update : updateList) {
	    if (update instanceof BlitScreenUpdate) {
		// If there is more than one BlitScreenUpdate, bail out
		if (lonelyUpdate != null) {
		    lonelyUpdate = null;
		    break;
		}
		lonelyUpdate = (BlitScreenUpdate) update;
	    }
	}

	return lonelyUpdate;
    }
}