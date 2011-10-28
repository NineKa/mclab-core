function [result] = u43_auto(n)
  x0 = char(32+80*[0.236048089737,0.103166034231;0.396058242611,0.154972270802]*n);
  x1 = uint64([0.237964627092,0.544229225296;0.369955166548,0.603920038596]*n);
  result = mldivide(x0,x1);
end
