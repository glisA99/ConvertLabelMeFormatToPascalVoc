package transformations;

import model.Image;
import model.Label;

@FunctionalInterface
interface IRectify {

    /**
     * Rectify output polygons to Pascal VOC Bounding boxes:
     * (xmin-top left, ymin-top left,xmax-bottom right, ymax-bottom right)
     *
     * @param image - image parsed from json file
     *
     */
    Image rectify(Image image);

}

public class Rectify implements IRectify {

    private static Rectify instance;

    private Rectify() {}

    public static Rectify getInstance() {
        if (instance == null) instance = new Rectify();
        return instance;
    }

    public Image rectify(Image image) {
        image.getLabels().forEach(label -> {
            double[] rectifiedPolygon = _rectify(label);
            image.getRectifiedPolygons().add(rectifiedPolygon);
        });
        return image;
    }

    private double[] _rectify(Label label) {
        double[][] points = label.getPoints();
        double xmin = 10000,xmax = 0,ymin = 10000,ymax = 0;

        for(int i = 0;i < points.length;i++) {
            // extract x and y cordinates for each polygon point
            double xcord = points[i][0], ycord = points[i][1];
            System.out.println("xcord: " + xcord + "\t" + "ycord: " + ycord);
            // update if needed xmax and xmin
            if (xcord > xmax) xmax = xcord;
            if (xcord < xmin) xmin = xcord;
            // update if needed ymax and ymin
            if (ycord > ymax) ymax = ycord;
            if (ycord < ymin) ymin = ycord;
        }

        return new double[] { xmin, ymin, xmax, ymax };
    }

}
