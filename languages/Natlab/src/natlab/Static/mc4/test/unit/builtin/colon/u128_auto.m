function [result] = u128_auto(n)
  x0 = int8([0.956034271889]*n);
  x1 = uint64([0.237964627092]*n);
  x2 = int8([0.956034271889]*n);
  result = colon(x0,x1,x2);
end
