package pl.sda.gdajava25.NBP_API.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
@ToString
@NoArgsConstructor
@Getter
@Setter
@XmlRootElement(name = "Rate")
public class Rate {

    @XmlElement(name = "No")
    private String No;

    @XmlElement(name = "EffectiveDate")
    private String EffectiveDate;

    @XmlElement(name = "Bid")
    private Double Bid;

    @XmlElement(name = "Ask")
    private Double Ask;

    @XmlElement(name="Mid")
    private Double Mid;

}
