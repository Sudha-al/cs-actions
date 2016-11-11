package io.cloudslang.content.xml.services;

import com.google.gson.*;
import io.cloudslang.content.xml.entities.inputs.ConvertXmlToJsonInputs;
import io.cloudslang.content.xml.utils.XmlUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static io.cloudslang.content.xml.utils.Constants.*;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Created by ursan on 8/2/2016.
 */
public class ConvertXmlToJsonService {
    private final StringBuilder namespacesPrefixes;
    private final StringBuilder namespacesUris;

    public ConvertXmlToJsonService() {
        namespacesPrefixes = new StringBuilder();
        namespacesUris = new StringBuilder();
    }

    public String convertToJsonString(final ConvertXmlToJsonInputs inputs) throws JDOMException, IOException, SAXException {
        if (StringUtils.isBlank(inputs.getXml())) {
            return EMPTY_STRING;
        }
        final InputSource inputSource = new InputSource(new StringReader(inputs.getXml()));
        final SAXBuilder builder = new SAXBuilder();
        XmlUtils.setFeatures(builder, inputs.getParsingFeatures());
        final Document document = builder.build(inputSource);
        final Element root = document.getRootElement();
        final List<Element> xmlElements = Collections.singletonList(root);
        JsonObject jsonObject = convertXmlElementsToJsonObject(xmlElements, inputs.getIncludeAttributes(), inputs.getTextElementsName());
        jsonObject = getJsonObjectWithRootElement(root, jsonObject, inputs.getIncludeRootElement());

        return inputs.getPrettyPrint() ? prettyPrint(jsonObject) : jsonObject.toString();
    }

    private JsonObject getJsonObjectWithRootElement(final Element rootElement, final JsonObject jsonObject, final boolean addRootElement) {
        if (addRootElement) {
            final JsonObject rootJson = new JsonObject();
            rootJson.add(rootElement.getName(), jsonObject);
            return rootJson;
        }
        return jsonObject;
    }

    private String prettyPrint(final JsonObject rootJson) {
        final Gson gson = new GsonBuilder().setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
        return gson.toJson(rootJson);
    }

    private JsonObject addJsonObjectsAndPrimitives(final JsonObject jsonObject, final List<Element> elements, final boolean includeAttributes, final String textPropName) {
        for (final Element element : elements) {
            final JsonElement jsonElement = isPrimitiveElement(element) ?
                    new JsonPrimitive(element.getValue()) : convertXmlElementsToJsonObject(Collections.singletonList(element), includeAttributes, textPropName);
            jsonObject.add(getElementFullName(element), jsonElement);
        }
        return jsonObject;
    }


    private JsonObject convertXmlElementsToJsonObject(List<Element> xmlElements, final boolean includeAttributes, final String textPropName) {
        JsonObject result = new JsonObject();
        for (final Element xmlElement : xmlElements) {
            result = addXmlElementToJsonObject(result, xmlElement, includeAttributes, textPropName);
        }
        return result;
    }

    private JsonArray convertXmlElementsToJsonArray(final List<Element> xmlElements, final boolean includeAttributes, final String textPropName) {
        final JsonArray result = new JsonArray();
        for (final Element xmlElement : xmlElements) {
            result.add(addXmlElementToJsonObject(new JsonObject(), xmlElement, includeAttributes, textPropName));
        }
        return result;
    }

    private JsonObject addXmlElementToJsonObject(JsonObject jsonObject, final Element xmlElement, final Boolean includeAttributes, final String textPropName) {
        addNamespaces(xmlElement.getAdditionalNamespaces());
        if (includeAttributes) {
            jsonObject = addAttributesToJsonObject(jsonObject, xmlElement.getAttributes());
        }
        List<Element> children = xmlElement.getChildren();
        jsonObject = searchAndAddArrayElementsToJsonObject(jsonObject, children, includeAttributes, textPropName);
        children = eliminateArrayElements(children);
        jsonObject = addJsonObjectsAndPrimitives(jsonObject, children, includeAttributes, textPropName);
        jsonObject = addTextProp(jsonObject, xmlElement.getText(), textPropName);
        return jsonObject;
    }

    private JsonObject searchAndAddArrayElementsToJsonObject(final JsonObject jsonObject, final List<Element> elements, final boolean includeAttributes, final String textPropName) {
        final List<String> arrayElementsNames = getListOfArrayElementNames(elements);
        for (final String arrayName : arrayElementsNames) {
            List<Element> arrayElements = getElementsByName(arrayName, elements);
            jsonObject.add(arrayName, convertXmlElementsToJsonArray(arrayElements, includeAttributes, textPropName));
        }
        return jsonObject;
    }

    private List<Element> eliminateArrayElements(List<Element> elements) {
        final List<String> arrayElementsNames = getListOfArrayElementNames(elements);
        for (final String arrayName : arrayElementsNames) {
            elements = eliminateElementsWithName(arrayName, elements);
        }
        return elements;
    }

    private JsonObject addTextProp(final JsonObject jsonObject, final String text, final String textPropName) {
        if (isNotEmpty(text) && text.matches(".*[a-zA-Z0-9].*")) {
            jsonObject.addProperty(textPropName, text);
        }
        return jsonObject;
    }

    private void addNamespaces(final List<Namespace> namespaces) {
        for (final Namespace namespace : namespaces) {
            if (namespacesUris.length() > 0) {
                namespacesPrefixes.append(Defaults.DELIMITER);
                namespacesUris.append(Defaults.DELIMITER);
            }
            namespacesPrefixes.append(namespace.getPrefix());
            namespacesUris.append(namespace.getURI());
        }
    }

    private JsonObject addAttributesToJsonObject(final JsonObject jsonObject, final List<Attribute> attributes) {
        for (final Attribute attribute : attributes) {
            jsonObject.addProperty(JSON_ATTRIBUTE_PREFIX + attribute.getName(), attribute.getValue());
        }
        return jsonObject;
    }

    private String getElementFullName(Element element) {
        String name = element.getNamespacePrefix();
        if (!name.isEmpty())
            name += Defaults.PREFIX_DELIMITER;
        name += element.getName();
        return name;
    }

    private List<String> getElementsFullName(List<Element> elements) {
        List<String> names = new LinkedList<>();
        for (Element element : elements) {
            names.add(getElementFullName(element));
        }
        return names;
    }


    private Boolean isPrimitiveElement(Element element) {
        return element.getChildren().isEmpty() && element.getAttributes().isEmpty(); //if it doesn't have child and doesn't have attributes it's primitive.
    }

    private List<String> getListOfArrayElementNames(List<Element> elements) {
        List<String> names = new LinkedList<>();
        List<String> elementsStr = getElementsFullName(elements);
        for (String name : elementsStr) {
            if (elementsStr.indexOf(name) != elementsStr.lastIndexOf(name) && !names.contains(name)) {
                names.add(name);
            }
        }
        return names;
    }

    private List<Element> getElementsByName(String arrayName, List<Element> elementsToSearch) {
        final List<Element> elements = new LinkedList<>();
        for (final Element element : elementsToSearch) {
            if (getElementFullName(element).equals(arrayName)) {
                elements.add(element);
            }
        }
        return elements;
    }

    private List<Element> eliminateElementsWithName(final String arrayName, final List<Element> elements) {
        final List<Element> newElements = new LinkedList<>();
        for (final Element element : elements) {
            if (!getElementFullName(element).equals(arrayName)) {
                newElements.add(element);
            }
        }
        return newElements;
    }

    public String getNamespacesUris() {
        return namespacesUris.toString();
    }

    public String getNamespacesPrefixes() {
        return namespacesPrefixes.toString();
    }
}
