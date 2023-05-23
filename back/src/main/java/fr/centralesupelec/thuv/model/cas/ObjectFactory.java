//
// Ce fichier a xe9txe9 gxe9nxe9rxe9 par l'implxe9mentation de rxe9fxe9rence
// JavaTM Architecture for XML Binding (JAXB), v2.3.1-b171012.0423
// Voir <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a>
// Toute modification apportxe9e xe0 ce fichier sera perdue lors de la recompilation du schxe9ma source.
// Gxe9nxe9rxe9 le : 2018.10.14 xe0 05:55:23 PM CEST
//


package fr.centralesupelec.thuv.model.cas;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;

import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the fr.centralesupelec.thuv.model.cas package.
 *
 * <p>An ObjectFactory allows you to programatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups.  Factory methods for each of these are
 * provided in this class.
 *
 */
@XmlRegistry
public class ObjectFactory {

    private static final QName _ServiceResponse_QNAME =
            new QName("http://www.yale.edu/tp/cas", "serviceResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package:
     *      fr.centralesupelec.thuv.model.cas
     *
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link ServiceResponseType }.
     *
     */
    public ServiceResponseType createServiceResponseType() {
        return new ServiceResponseType();
    }

    /**
     * Create an instance of {@link AttributesType }.
     *
     */
    public AttributesType createAttributesType() {
        return new AttributesType();
    }

    /**
     * Create an instance of {@link AuthenticationSuccessType }.
     *
     */
    public AuthenticationSuccessType createAuthenticationSuccessType() {
        return new AuthenticationSuccessType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ServiceResponseType }{@code >}.
     *
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link ServiceResponseType }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.yale.edu/tp/cas", name = "serviceResponse")
    public JAXBElement<ServiceResponseType> createServiceResponse(ServiceResponseType value) {
        return new JAXBElement<ServiceResponseType>(_ServiceResponse_QNAME, ServiceResponseType.class, null, value);
    }

}
