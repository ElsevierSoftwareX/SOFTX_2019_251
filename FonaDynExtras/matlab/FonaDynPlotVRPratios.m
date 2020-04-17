function FonaDynPlotVRPratios(sArray1, sArray2, colNames, colName, fig, varargin)
%% function FonaDynPlotVRPratios(sArray1, sArray2, colNames, colName, fig, ...)
% Given two input VRPs, plot a VRP in which each cell has the value
% of the ratio of the corresponding cells in the input VRPs, for assessing
% changes. 
% <sArray> are arrays of numbers and <colNames> is a cell array of column names, 
% both previously returned by FonaDynLoadVRP.m. 
% <colName> is the name of the column to be plotted (case sensitive).
% <fig> is the number of the current figure or subplot.
% Optional arguments: 
% 'MinCycles', integer       - set a minimum cycles-per-cell threshold
% 'Range', [foMin, foMax, Lmin, Lmax]   - specify plot range
% 'OffsetSPL', value         - offset the dB SPL (for calibration)
% 'ColorBar', 'on' or 'off'  - show a horizontal color bar at the top left

minCycles = 1;
xmax = 80;  % can be <=96
xmin = 40;  % can be >=30
ymax = 120; % can be <=120
ymin = 50;  % can be >=40
offsetSPL = 0;  % useful for SPL re-calibration
plotHz = 0;
bColorBar = 0;
cbLabel = '';
tickLabels = {}; 
ticks = [];

args = nargin-5;

% Handle any optional arguments
for i = 1 : 2 : args
    switch varargin{i}
        case 'MinCycles'
            minCycles = varargin{i+1};
        case 'Range'
            range = varargin{i+1};
            xmin = range(1);
            xmax = range(2);
            ymin = range(3);
            ymax = range(4);
        case 'OffsetSPL'
            offsetSPL = varargin{i+1};
        case 'ColorBar'
            if 'on' == lower(varargin{i+1})
                bColorBar = 1;
            end
        case 'PlotHz'
            if 'on' == lower(varargin{i+1})
                plotHz = 1;
            end
        otherwise
            warning (['Unrecognized option: ' varargin{i}]);
    end
end

colPlot = find(ismember(colNames, colName)); 
if size(colPlot,2) < 1
    warning(['Unrecognized column name: ' colName]);
    return
end
colMaxCluster = find(ismember(colNames, 'maxCluster')); 
colTotal = find(ismember(colNames, 'Total')); 
nClusters = size (sArray1, 2) - colMaxCluster;

figure(fig);
axH = gca;

% Set up a colormap from red to green through near-white
%[colors, cmin, cmax] = FonaDynColors(colName, nClusters);
R1 = linspace(0.97, 0.97, 32); 
R2 = linspace(0.97, 0.0, 32);
G1 = linspace(0.0, 0.97, 32); 
G2 = linspace(0.97, 0.97, 32); 
B1 = linspace(0, 0.97, 32); 
B2 = linspace(0.97, 0, 32);
colors = [ [R1 R2] ; [G1 G2]; [B1 B2]]';
cmin = 0.5;
cmax = 1.5;
% cmin = -0.2;
% cmax =  0.2;


pix1 = ones(150,100)*NaN;
pix2 = ones(150,100)*NaN;
allPixels = ones(150,100)*NaN;

indices = find(sArray1(:, colTotal) >= minCycles);
for i=1:length(indices)
    y = sArray1(indices(i), 2) + round(offsetSPL);
    x = sArray1(indices(i), 1);
    z = sArray1(indices(i), colPlot);
    pix1(y, x) = z;
end

indices = find(sArray2(:, colTotal) >= minCycles);
for i=1:length(indices)
    y = sArray2(indices(i), 2) + round(offsetSPL);
    x = sArray2(indices(i), 1);
    z = sArray2(indices(i), colPlot);
    pix2(y, x) = z;
end

% Compute the per-element ratios pix2/pix1
% Works only when both are non-negative values; might fail for some dEGGnt's
diffs = ones(1);
ratios = ones(1);
diffIx = 1;
for x = xmin : xmax
    for y = ymin : ymax
        value1 = pix1(y, x);
        value2 = pix2(y, x); 
        if (value1 ~= NaN) & (value2 ~= NaN)
            if value1*value2 > 0
                ratio = value2/value1;
                allPixels(y, x) = ratio;
                ratios(diffIx) = ratio;
                diffs(diffIx) =  value2 - value1;
                diffIx = diffIx + 1; 
%                allPixels(y, x) = value2 - value1;
%             else
%                 allPixels(y, x) = 1.0;
            end
        end
    end
end

aststr = '';
[h,p] = ttest(diffs);
mdiff = mean(diffs);
gdiff = geomean(ratios);
diffstr = num2str(gdiff);
%diffstr = num2str(mdiff);
aststr = diffstr;
if h > 0
      if p <= 0.05
        aststr = [' *' diffstr];
        if p <= 0.01
            aststr = [' **' diffstr];
            if p <= 0.001
                aststr = [' ***' diffstr];
            end
        end
    end
end

% Plot the cells astride the grid, not next to it
X = (1:100)-0.5;
Y = (1:150)-0.5;
handle = pcolor(X,Y,allPixels);

% cFloor = 0.5*min(min(allPixels));
% cCeil  = 0.5*max(max(allPixels));
% cmax = max(abs(cFloor), abs(cCeil));
% cmin = -cmax;

colormap(axH, colors);
caxis(axH, [cmin cmax]);

set(handle, 'EdgeColor', 'none');
xlim(axH, [xmin xmax])
ylim(axH, [ymin ymax])
grid on

if bColorBar == 1
    cbLabel = ['GMean: ' aststr];
    cb = colorbar(axH);
    cb.Location = 'north';
    cb.Position(1) = axH.Position(1) + 0.01;
    cb.Position(2) = axH.Position(2) + axH.Position(4)+ 0.01; 
    cb.Position(3) = cb.Position(3) / 2.5;
    cb.Position(4) = cb.Position(4) / 2;
    cb.TickLength = 0.1; 
    if size(ticks) > 0
        cb.Ticks = ticks;
        cb.TickLabels = tickLabels;
    end
    cb.Label.String = ['   ' cbLabel];
    cb.Label.VerticalAlignment = 'top';
end

if plotHz == 1
    fMin = 220*2^((xmin-57)/12);
    fMax = 220*2^((xmax-57)/12); 
    if (fMax > 1600)
        step = 2;
    else 
        step = 1;
    end;
    ticks = [];
    tickLabels = {};
    ix = 1; 
    for j = 1 : step : 20
        i = (2^(j-1))*100;
        if (i >= fMin) & (i <= fMax)
            st = 57+12*log(i/220)/log(2);
            ticks(ix) = st;
            tickLabels(ix) = {num2str(i)};
            ix = ix + 1;
        end
    end
    axH.XTick = ticks;
    axH.XTickLabel = tickLabels;
end

end