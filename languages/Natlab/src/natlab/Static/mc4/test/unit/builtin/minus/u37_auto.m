function [result] = u37_auto(n)
  x0 = uint64([0.237964627092,0.544229225296;0.369955166548,0.603920038596]*n);
  x1 = uint64([0.237964627092,0.544229225296;0.369955166548,0.603920038596]*n);
  result = minus(x0,x1);
end
