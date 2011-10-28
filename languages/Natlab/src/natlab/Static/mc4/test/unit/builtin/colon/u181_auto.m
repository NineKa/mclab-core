function [result] = u181_auto(n)
  x0 = char(32+80*[0.236048089737]*n);
  x1 = single([0.844421851525]*n);
  x2 = double([0.134364244112]*n);
  result = colon(x0,x1,x2);
end
