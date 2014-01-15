%ip = '192.168.11.2';
%ip = '192.168.0.105';
ip = '128.179.164.144';
port = 8888;
run = true;

close all

t = tcpip(ip, port, 'NetworkRole', 'client');

% these delays are necessary for some reason
try
    fopen(t);
    pause(.1);
    fprintf(t, 'sensor start');
    pause(.1);
catch exception
    disp('Could not connect, did you launch the application?');
    return
end

figure('name','Internal accelerometer real-time view');

% add a stop button to the plot
buttonStop = uicontrol('Style', 'pushbutton', 'Callback', 'run = false;', 'String', 'Stop');

% real returned values
subplot(1,2,1);

h = bar([0 0 0],'r');
title('BOSCH BMA250');

% label axes and set limits
xlabel('Axis');
ylabel('Acceleration (m/s^2)');
set(gca,'XTickLabel',{'x', 'y', 'z'})
set(gca,'YLim',[-10 10]);

% 3d image of phone position
subplot(1,2,2);

title('Smartphone model');

% make the elongated box centered on the origin
my_vertices = [
    -1 -.5 -.12
    -1 .5 -.12
    1 .5 -.12
    1 -.5 -.12
    -1 -.5 .12
    -1 .5 .12
    1 .5 .12
    1 -.5 .12];

my_faces = [
    1 2 3 4
    2 6 7 3
    4 3 7 8
    1 5 8 4
    1 2 6 5
    5 6 7 8];

% make a 3D plot
view(3)
% set default rotation
view([90 0 0])
% add some perspective to make it look nice
camproj('perspective')

% force same length on all axis
axis vis3d
axis([-1 1 -1 1 -1 1 0 1])

% hide axis
axis off;

% make the actual 3D model
d = patch('Vertices', my_vertices, 'Faces', my_faces, 'FaceColor', 'b');

% original values used later to reset model rotation
reset = [get(d, 'XData'); get(d, 'YData'); get(d, 'ZData')];

% start sampling
fprintf(t, 'sensor acc');
pause(.1);

% as long as the connection is up loop here
while run
    if get(t, 'BytesAvailable') > 1
        % read data in, make the right format
        data = str2num(char(fread(t, t.BytesAvailable, 'char')'));
        % update plot
        if length(data) == 3
            try
                set(h, 'YData', data);
            catch error
                continue
            end
            
            % get new rotation angles for the 3D model
            x = data(1) / 9.8;
            y = data(2) / 9.8;
            angleX = asin(x)*180/pi;
            angleY = asin(y)*180/pi;
            
            if data(3) < 0
                angleX = angleX + 180;
                angleY = -angleY;
            end
            
            % reset box to initial view
            set(d, 'Xdata', reset(1:4,1:6));
            set(d, 'Ydata', reset(5:8,1:6));
            set(d, 'Zdata', reset(9:12,1:6));
            
            % rotate the model around the origin
            rotate(d, [1,0,0], angleX, [0 0 0]);
            rotate(d, [0,1,0], angleY, [0 0 0]);
            
            % force redraw
            drawnow
        end
        % ask for another data point
        fprintf(t, 'sensor acc');
    end
end

fclose(t);
delete(t);
clear t

close all
