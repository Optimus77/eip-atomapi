package com.inspur.eipatomapi.entity.eip;

import com.alibaba.druid.sql.ast.expr.SQLCaseExpr;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inspur.eipatomapi.entity.bss.EipOrderProduct;
import com.inspur.eipatomapi.entity.bss.EipOrderProductItem;
import com.inspur.eipatomapi.util.TypeConstraint;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.List;

@Data
public class EipAllocateParam implements Serializable {

    @NotBlank(message = "can not be blank.")
    private String region;

    @NotBlank(message = "can not be blank.")
    @TypeConstraint(allowedValues = {"5_bgp","5_sbgp", "5_telcom", "5_union"}, message = "Only 5_bgp,5_sbgp, 5_telcom, 5_union is allowed. ")
    private String iptype;

    @TypeConstraint(allowedValues = {"PrePaid","PostPaid"}, message = "Only PrePaid,PostPaid is allowed. ")
    private String chargetype = "PrePaid";

    @TypeConstraint(allowedValues = {"Bandwidth","SharedBandwidth"}, message = "Only Bandwidth,SharedBandwidth is allowed. ")
    private String chargemode = "Bandwidth";

    @Pattern(regexp="[0-9-]{1,2}", message="param purchase time error.")
    private String purchasetime;

    @Range(min=1,max=2000,message = "value must be 1-2000.")
    private int bandwidth;

    @JsonProperty("sharedbandwidthid")
    private String sharedBandWidthId;

    //以下新增字段属性
    private String userId;
    private String productLineCode = "EIP";
    private String setCount = "1";
    private String consoleOrderFlowId;
    private String billType ="monthly";
    private String duration;
    private String durationUnit = "M";
    private String orderWhat = "formal";
    private String orderType = "new";
    private String servStartTime;
    private String servEndTime;
    private String rewardActivity;
    private String consoleCustomization;
    private String totalMoney;

    private List<EipOrderProduct> productList;

    private List<EipOrderProductItem> itemList;

}
