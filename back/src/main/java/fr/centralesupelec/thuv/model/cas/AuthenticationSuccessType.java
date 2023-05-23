//
// Ce fichier a xe9txe9 gxe9nxe9rxe9 par l'implxe9mentation de rxe9fxe9rence
//  JavaTM Architecture for XML Binding (JAXB), v2.3.1-b171012.0423
// Voir <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a>
// Toute modification apportxe9e xe0 ce fichier sera perdue lors de la recompilation du schxe9ma source.
// Gxe9nxe9rxe9 le : 2018.10.14 xe0 05:55:23 PM CEST
//


package fr.centralesupelec.thuv.model.cas;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * Classe Java pour authenticationSuccessType complex type.
 *
 * <p>Le fragment de schxE9ma suivant indique le contenu attendu figurant dans cette classe.
 *
 * <pre>
 * &lt;complexType name="authenticationSuccessType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="user" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="attributes" type="{http://www.yale.edu/tp/cas}attributesType"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "authenticationSuccessType", propOrder = {
    "user",
    "attributes"
})
public class AuthenticationSuccessType {

    @XmlElement(required = true)
    protected String user;
    @XmlElement(required = true)
    protected AttributesType attributes;

    /**
     * Obtient la valeur de la proprixE9txE9 user.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getUser() {
        return user;
    }

    /**
     * DxE9finit la valeur de la proprixE9txE9 user.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setUser(String value) {
        this.user = value;
    }

    /**
     * Obtient la valeur de la proprixE9txE9 attributes.
     *
     * @return
     *     possible object is
     *     {@link AttributesType }
     *
     */
    public AttributesType getAttributes() {
        return attributes;
    }

    /**
     * DxE9finit la valeur de la proprixE9txE9 attributes.
     *
     * @param value
     *     allowed object is
     *     {@link AttributesType }
     *
     */
    public void setAttributes(AttributesType value) {
        this.attributes = value;
    }

}
