package com.jbm.sample;

import java.io.File;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.name.Rename;

@Service
public class ImageProcessService {

	@PostConstruct
	public void process() throws IOException {
		Thumbnails.of(new File("美图图库").listFiles()).sourceRegion(200, 200, 50, 50).size(50, 50).outputFormat("png").toFiles(Rename.PREFIX_DOT_THUMBNAIL);
	}
}
