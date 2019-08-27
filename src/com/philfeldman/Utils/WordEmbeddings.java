package com.philfeldman.Utils;

import com.philfeldman.utils.xmlParsing.Dom4jUtils;
import org.dom4j.DocumentException;
import org.dom4j.Element;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class WordEmbeddings{
    protected List<WordEmbedding> embeddings;
    public boolean loadSuccess;

    public WordEmbeddings() {
        embeddings = new ArrayList<>();
        loadSuccess = false;
    }

    public List<WordEmbedding> getEmbeddings(){
        return embeddings;
    }

    public WordEmbedding getWordEmbedding(int i){
        if(i >= 0 && i < embeddings.size()){
            return embeddings.get(i);
        }
        return null;
    }

    public int getNumEmbeddings(){
        return embeddings.size();
    }

    public void clear() {
        embeddings.clear();
    }

    public boolean loadFromXML(File file) {
        loadSuccess = true;
        List<Element> eList = new ArrayList<>();
        Dom4jUtils d4ju = new Dom4jUtils();
        try {
            d4ju.create(file);
            eList = d4ju.findNodesByFullName("root.entry");
            for (Element e : eList){
                WordEmbedding we = new WordEmbedding(e);
                embeddings.add(we);
            }

        }catch (DocumentException e) {
            e.printStackTrace();
            loadSuccess = false;
        }

        return loadSuccess;
    }
}
