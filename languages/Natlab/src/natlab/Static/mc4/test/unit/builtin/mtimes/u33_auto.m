function [result] = u33_auto(n)
  x0 = int8([0.956034271889,0.947827487059;0.0565513677268,0.0848719951589]*n);
  x1 = ([0.62290169489,0.741786989261;0.795193565566,0.942450283777]*n > .5);
  result = mtimes(x0,x1);
end
