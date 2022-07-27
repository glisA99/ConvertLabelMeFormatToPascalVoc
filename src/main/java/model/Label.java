package model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor @AllArgsConstructor
public class Label {

    private String label;
    private double[][] points = new double[4][2];
    private String shape_type;

}
