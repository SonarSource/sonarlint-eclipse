package devoxx;

public class QuizzUnreachableConditionalBranch {

  public int ultimateBoringBusinessLogic(
      int antonioAge, int nicolasAge, int zouheirAge) {
    
    if (antonioAge > nicolasAge 
        && nicolasAge == zouheirAge 
        && zouheirAge > antonioAge) {
      return Integer.MAX_VALUE;
    }
    
    return -1;
  }  
}
