function [result] = u110_auto(n)
  x0 = int8([0.956034271889]*n);
  x1 = single([0.844421851525]*n);
  x2 = int8([0.956034271889]*n);
  result = colon(x0,x1,x2);
end
