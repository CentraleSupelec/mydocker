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
 * Classe Java pour attributesType complex type.
 *
 * <p>Le fragment de schxE9ma suivant indique le contenu attendu figurant dans cette classe.
 *
 * <pre>
 * &lt;complexType name="attributesType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="uid" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="simpleName" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="email" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="authenticationDate" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="isFromNewLogin" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="longTermAuthenticationRequestTokenUsed" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "attributesType", propOrder = {
    "uid",
    "simpleName",
    "email",
    "authenticationDate",
    "isFromNewLogin",
    "longTermAuthenticationRequestTokenUsed"
})
public class AttributesType {

    @XmlElement(required = true)
    protected String uid;
    @XmlElement(required = true)
    protected String simpleName;
    @XmlElement(required = true)
    protected String email;
    @XmlElement(required = true)
    protected String authenticationDate;
    @XmlElement(required = true)
    protected String isFromNewLogin;
    @XmlElement(required = true)
    protected String longTermAuthenticationRequestTokenUsed;

    /**
     * Obtient la valeur de la proprixE9txE9 uid.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getUid() {
        return uid;
    }

    /**
     * DxE9finit la valeur de la proprixE9txE9 uid.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setUid(String value) {
        this.uid = value;
    }

    /**
     * Obtient la valeur de la proprixE9txE9 simpleName.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getSimpleName() {
        return simpleName;
    }

    /**
     * DxE9finit la valeur de la proprixE9txE9 simpleName.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setSimpleName(String value) {
        this.simpleName = value;
    }

    /**
     * Obtient la valeur de la proprixE9txE9 email.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getEmail() {
        return email;
    }

    /**
     * DxE9finit la valeur de la proprixE9txE9 email.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setEmail(String value) {
        this.email = value;
    }

    /**
     * Obtient la valeur de la proprixE9txE9 authenticationDate.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getAuthenticationDate() {
        return authenticationDate;
    }

    /**
     * DxE9finit la valeur de la proprixE9txE9 authenticationDate.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setAuthenticationDate(String value) {
        this.authenticationDate = value;
    }

    /**
     * Obtient la valeur de la proprixE9txE9 isFromNewLogin.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getIsFromNewLogin() {
        return isFromNewLogin;
    }

    /**
     * DxE9finit la valeur de la proprixE9txE9 isFromNewLogin.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setIsFromNewLogin(String value) {
        this.isFromNewLogin = value;
    }

    /**
     * Obtient la valeur de la proprixE9txE9 longTermAuthenticationRequestTokenUsed.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getLongTermAuthenticationRequestTokenUsed() {
        return longTermAuthenticationRequestTokenUsed;
    }

    /**
     * DxE9finit la valeur de la proprixE9txE9 longTermAuthenticationRequestTokenUsed.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setLongTermAuthenticationRequestTokenUsed(String value) {
        this.longTermAuthenticationRequestTokenUsed = value;
    }

}
