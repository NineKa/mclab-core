function [result] = u55_auto(n)
  x0 = single([0.844421851525]*n);
  x1 = uint64([0.237964627092]*n);
  x2 = double([0.134364244112]*n);
  result = colon(x0,x1,x2);
end
