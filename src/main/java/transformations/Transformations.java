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
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import static constants.JsonFormatConstants.*;
import static constants.XMLConstants.*;

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
    public static void convertJsonToPascalVoc(String filepath, String imageExtension, String dbName) throws IOException, ParseException, TransformerException {
        // parse json to model Image
        Image image = parseJson(filepath);

        // rectify polygons (rectify method mutates original image)
        Rectify.getInstance().rectify(image);

        // convert to corresponding xml element (annotation)
        Document document = convertImageToXML(image,filepath,imageExtension,dbName);

        // write
        String directoryPath = extractDirectoryPath(filepath);
        String filename = extractFilename(filepath);
        String _path = directoryPath + "/" + filename + "." + "xml";
        writeDocument(document,_path);
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

    private static String extractDirectoryPath(String filepath) {
        System.out.println("Filepath: " + filepath);
        String[] split = filepath.split("/");
        String concatString = "";
        for(int i = 0;i < split.length - 1;i++) {
            concatString = concatString.concat(i == 0 ? "./" + split[i] : "/" + split[i]);
        }
        System.out.println("Directory path: " + concatString);
        return concatString;
    }

    private static String extractFilename(String filepath) {
        String[] split = filepath.split("/");
        String filename = split[split.length - 1];
        split = filename.split("\\.");
        System.out.println("Filename: " + split[0]);
        return split[0];
    }

    public static Document convertImageToXML(Image image, String filepath, String imageExtension, String dbName) {
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
            System.out.println("Filename: " + filename);
            String imageFilename = filename.split("\\.")[0] + imageExtension;
            filenameEl.setTextContent(imageFilename);
            annotations.appendChild(filenameEl);

            // path element
            Element pathEl = document.createElement(XML_PATH);
            split = filepath.split("/");
            String concatString = "";
            for(int i = 0;i < split.length - 1;i++) {
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
            for(int i = 0;i < image.getLabels().size();i++) {
                Label label = ((ArrayList<Label>) image.getLabels()).get(i);
                Element objectEl = document.createElement(XML_OBJECT);

                Element nameEl = document.createElement(XML_OBJECT_NAME);
                nameEl.setTextContent(label.getLabel());
                objectEl.appendChild(nameEl);

                Element poseEl = document.createElement(XML_OBJECT_POSE);
                poseEl.setTextContent("Unspecified");
                objectEl.appendChild(poseEl);

                Element trucatedEl = document.createElement(XML_OBJECT_TRUNCATED);
                trucatedEl.setTextContent("0");
                objectEl.appendChild(trucatedEl);

                Element diffEl = document.createElement(XML_OBJECT_DIFFICULT);
                diffEl.setTextContent("0");
                objectEl.appendChild(diffEl);

                // bnd box
                Element bndboxEl = document.createElement(XML_OBJECT_BNDBOX);
                double[] rect = ((ArrayList<double[]>) image.getRectifiedPolygons()).get(i);
                Element xminEl = document.createElement(XML_OBJECT_BNDBOX_XMIN);
                Element yminEl = document.createElement(XML_OBJECT_BNDBOX_YMIN);
                Element xmaxEl = document.createElement(XML_OBJECT_BNDBOX_XMAX);
                Element ymaxEl = document.createElement(XML_OBJECT_BNDBOX_YMAX);
                Element[] cords = new Element[] { xminEl, yminEl, xmaxEl, ymaxEl };
                for(int j = 0;j < cords.length;j++) {
                    cords[j].setTextContent(Double.toString(rect[j]));
                    bndboxEl.appendChild(cords[j]);
                }
                objectEl.appendChild(bndboxEl);

                annotations.appendChild(objectEl);
            };

            document.appendChild(annotations);
            return document;
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new RuntimeException(exception);
        }
    }

    public static void writeDocument(Document document, String full_path) throws TransformerException {
        // write content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(document);

        StreamResult result = new StreamResult(new File(full_path));

        transformer.transform(source, result);
        System.out.println("File created successfully");
    }

}
