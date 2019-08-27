package com.philfeldman.Utils;

import com.philfeldman.utils.datesAndTime.CalendarParser;
import com.philfeldman.utils.datesAndTime.CalendarParserException;
import org.dom4j.Element;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static com.philfeldman.utils.datesAndTime.CalendarParser.*;

/**
 * Created by philip.feldman on 1/20/2017.
 */
public class XmlLoadable {

    protected Date parseDate(String dstr) throws ParseException {
        int[] formats = {MM_DD_YY, DD_MM_YY, MM_YY_DD, DD_YY_MM, YY_DD_MM, YY_MM_DD};
        for(int f : formats) {
            try {
                Calendar cal = CalendarParser.parse(dstr, f);
                return cal.getTime();
            } catch (CalendarParserException e) {
                System.out.println("XmlLoadable.parseDate(): Unable to format "+dstr+" using format ["+f+"]");
            }
        }
        System.out.println("XmlLoadable.parseDate(): Unable to format "+dstr+" using any format");
        return null;
    }

    /**
     * Overload this for handling enums and such
     * @param f
     * @param varName
     * @param valStr
     */
    protected void handleSpecial(Field f, String varName, String valStr){

    }

    public boolean loadFromXML(Element root){
        Class c = this.getClass();
        Field[] fieldArray = c.getDeclaredFields();
        List<Element> eList = root.elements();
        HashMap<String, Element> eMap = new HashMap<>();
        for(Element e : eList){
            String name = e.getName();
            eMap.put(name, e);
        }

        for (Field f : fieldArray) {
            try {
                String n = f.getName().trim();
                Type t = f.getGenericType();
                String[] tnames = t.getTypeName().split("\\.");
                String tname = tnames[tnames.length-1].toLowerCase();
                if(!eMap.keySet().contains(n)){
                    continue;
                }
                Element e = eMap.get(n);
                try {

                    switch (tname) {
                        case "int": {
                            int val = Integer.parseInt(e.getText().trim());
                            f.setInt(this, val);
                            break;
                        }
                        case "double": {
                            double val = Double.parseDouble(e.getText().trim());
                            f.setDouble(this, val);
                            break;
                        }
                        case "float": {
                            float val = Float.parseFloat(e.getText().trim());
                            f.setDouble(this, val);
                            break;
                        }
                        case "date":
                            Date d = parseDate(e.getText());
                            f.set(this, d);
                            break;
                        case "boolean": {
                            boolean val = Boolean.parseBoolean(e.getText().trim());
                            f.setBoolean(this, val);
                            break;
                        }
                        case "string": {
                            f.set(this, e.getText().trim());
                            break;
                        }
                        default:
                            handleSpecial(f, n, e.getText().trim());
                            break;
                    }
                }catch(IllegalArgumentException ex){
                    handleSpecial(f, n, e.getText().trim());
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
                return false;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return false;
            } catch (ParseException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }
}
