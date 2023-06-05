package Common.Entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Seller
{
    @JsonProperty("id")
    public long id;
    @JsonProperty("name")
    public String name;
    @JsonProperty("order_count")
    public int order_count;

    public Seller() {
        this.id = 0;
        this.name = "";
        this.order_count = 0;
    }

//    public long getId() {
//        return id;
//    }
//
//    public void setId(long id) {
//        this.id = id;
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    public int getOrder_count() {
//        return order_count;
//    }
//
//    public void setOrder_count(int order_count) {
//        this.order_count = order_count;
//    }


    @Override
    public String toString() {
        return "Seller{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", order_count=" + order_count +
                '}';
    }
}
