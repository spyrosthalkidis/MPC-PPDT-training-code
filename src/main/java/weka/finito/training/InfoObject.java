package weka.finito.training;


import weka.core.Attribute;

public class InfoObject {
    private Attribute m_attribute;
    private String m_value;

    public InfoObject(Attribute attr, String val) {
        m_attribute = attr;
        m_value = val;
    }

    public Attribute returnAttribute() {
        return m_attribute;
    }

    public String returnValue() {
        return m_value;
    }
}