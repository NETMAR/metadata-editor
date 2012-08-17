package no.met.metadataeditor.dataTypes.attributes;

import no.met.metadataeditor.dataTypes.DataType;
import no.met.metadataeditor.dataTypes.IsAttributeValue;

public class LatLonBBSingleAttribute extends DataAttribute {

    @IsAttributeValue(DataType.STRING)
    String latLonStr;

}