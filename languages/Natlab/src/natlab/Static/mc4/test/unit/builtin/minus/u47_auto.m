function [result] = u47_auto(n)
  x0 = ([0.62290169489,0.741786989261;0.795193565566,0.942450283777]*n > .5);
  x1 = double([0.134364244112,0.847433736937;0.763774618977,0.255069025739]*n);
  result = minus(x0,x1);
end
