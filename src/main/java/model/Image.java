package model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Image {

    private Collection<Label> labels = new ArrayList<>();
    private Collection<double[]> rectifiedPolygons = new ArrayList<>();
    private String imagePath;
    private Long imageHeight;
    private Long imageWidth;

}
