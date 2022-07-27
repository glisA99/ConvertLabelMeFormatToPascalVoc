package transformations;

import model.Image;
import model.Label;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileReader;
import java.io.IOException;

import static constants.JsonFormatConstants.*;

/**
 *
 * Pascal Visual Object Classes (VOC)
 *
 * Pascal VOC provides standardized image data sets for object detection
 * In pascal voc we create a file for each of the image in the dataset.
 *
 * Pascal VOC Bounding box :(xmin-top left, ymin-top left,xmax-bottom right, ymax-bottom right)
 *
 * Key tags in PascalVOC format:
 * 1. folder
 * - folder that contains the images
 * 2. filename
 * - name of the physical file that exists in the folder
 * 3. size
 * - Contain the size of the image in terms of width, height and depth.
 *   If the image is black and white then the depth will be 1. For color images, depth will be 3
 * 4. object
 * - Contains the object details.
 *   If you have multiple annotations then the object tag with its contents is repeated.
 *   The components of the object tags are:
 *   1) name - name of the object that we are trying to identify
 *   2) pose
 *   3) truncated - bounding box specified for the object does not correspond to the full extent of the object
 *   4) difficult - object is considered difficult to recognize
 *   5) bndbox - axis-aligned rectangle specifying the extent of the object visible in the image
 *
 *  **Due to conversion information loss, next params are lost:
 *  -> pose of object
 *  -> truncated flag of object
 *  -> difficult flag of object
 *
 */
public final class Transformations {

    public static final String XML_ANNOTATION = "annotation";
    public static final String XML_FOLDER = "folder";
    public static final String XML_FILENAME = "filename";
    public static final String XML_PATH = "path";
    public static final String XML_SOURCE = "source";

    public static final String XML_SIZE = "size";
    public static final String XML_SIZE_WIDTH = "width";
    public static final String XML_SIZE_HEIGHT = "height";
    public static final String XML_SIZE_DEPTH = "depth";

    public static final String XML_OBJECT = "object";
    public static final String XML_OBJECT_NAME = "name";
    public static final String XML_OBJECT_POSE = "pose";
    public static final String XML_OBJECT_TRUNCATED = "truncated";
    public static final String XML_OBJECT_DIFFICULT = "difficult";
    public static final String XML_OBJECT_BNDBOX = "bndbox";

    /**
     * Convert image annotation in json format (output from labelme image annotation tool) to
     * corresponding PascalVOC XML format
     *
     * @param filepath file path (absolute or relative) to json file.
     * @param imageExtension image extension (jpg,png,jpeg,etc.)
     * @throws IOException
     * @throws ParseException
     *
     * @implNote directory where json file is store is assumed to be also directory of corresponding
*                images and output directory. Also image filename is expected to be same as
     *           input json filename.
     */
    public static void convertJsonToPascalVoc(String filepath, String imageExtension, String dbName) throws IOException, ParseException {
        // parse json to model Image
        Image image = parseJson(filepath);

        // convert to corresponding xml element (annotation)
        Element rootElement = convertImageToXML(image,filepath,imageExtension);
    }

    public static Image parseJson(String filepath) throws IOException, ParseException {
        FileReader fileReader = new FileReader(filepath);
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(fileReader);

        Image image = new Image();
        image.setImagePath((String) jsonObject.get(IMAGE_PATH));
        image.setImageHeight((Long) jsonObject.get(IMAGE_HEIGHT));
        image.setImageWidth((Long) jsonObject.get(IMAGE_WIDTH));

        JSONArray shapes = (JSONArray) jsonObject.get(SHAPES);
        shapes.stream().forEach(shape -> {
            JSONObject _shape = (JSONObject) shape;
            Label label = new Label();
            label.setShape_type((String) _shape.get(SHAPE_TYPE));
            label.setLabel((String) _shape.get(LABEL));

            JSONArray points = (JSONArray) _shape.get(POINTS);
            for(int i = 0;i < points.size();i++) {
                JSONArray pair = (JSONArray) points.get(i);
                label.getPoints()[i][0] = (double) pair.get(0);
                label.getPoints()[i][1] = (double) pair.get(1);
            }

            image.getLabels().add(label);
        });

        return image;
    }

    public static Element convertImageToXML(Image image, String filepath, String imageExtension, String dbName) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();

            // extract directory name
            String[] split = filepath.split("/");
            String directoryName = split[split.length - 1];
            // extract filename
            split = filepath.split("/");
            String filename = split[split.length - 1];

            // root element
            Element annotations = document.createElement(XML_ANNOTATION);

            // folder element
            Element folderEl = document.createElement(XML_FOLDER);
            folderEl.setTextContent(directoryName);
            annotations.appendChild(folderEl);

            // filename element
            Element filenameEl = document.createElement(XML_FILENAME);
            String imageFilename = filename.split(".")[0] + imageExtension;
            filenameEl.setTextContent(imageFilename);
            annotations.appendChild(filenameEl);

            // path element
            Element pathEl = document.createElement(XML_PATH);
            split = filepath.split("/");
            String concatString = "";
            for(int i = 0;i < split.length - 2;i++) {
                concatString.concat(i == 0 ? "./" + split[i] : "/" + split[i]);
            }
            concatString.concat("/" + imageFilename);
            pathEl.setTextContent(concatString);
            annotations.appendChild(pathEl);

            // source element
            Element sourceEl = document.createElement(XML_SOURCE);
            Element database = document.createElement("database");
            database.setTextContent(dbName);
            sourceEl.appendChild(database);
            annotations.appendChild(sourceEl);

            // size element
            Element sizeEl = document.createElement(XML_SIZE);
            Element widthEl = document.createElement(XML_SIZE_WIDTH);
            Element heightEl = document.createElement(XML_SIZE_HEIGHT);
            Element depthEl = document.createElement(XML_SIZE_DEPTH);
            widthEl.setTextContent(image.getImageWidth().toString());
            heightEl.setTextContent(image.getImageHeight().toString());
            depthEl.setTextContent("3");
            sizeEl.appendChild(widthEl);
            sizeEl.appendChild(heightEl);
            sizeEl.appendChild(depthEl);
            annotations.appendChild(sizeEl);

            // segmented element
            Element segmentedEl = document.createElement("segmented");
            segmentedEl.setTextContent("0");
            annotations.appendChild(segmentedEl);

            // object elements
            image.getLabels().forEach(label -> {
                Element objectEl = document.createElement(XML_OBJECT);

                annotations.appendChild(objectEl);
            });

            return annotations;
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new RuntimeException(exception);
        }
    }

}
