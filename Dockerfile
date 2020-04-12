FROM openkbs/jdk-mvn-py3-x11
COPY . /CS1660/project
WORKDIR /CS1660/project
CMD ["java", "-jar", "UI.jar","ya29.a0Ae4lvC2YdY2Kx3n3MkyixImy9iBVJR_vFh0mYBeiaFin9oF7rK7MjOChF30m04qQPm99OTqNFewzoZVgEiM74c5I_jyU6W0EtJiYpD6eNQIlHwprFg1qiy3dKZz6r0GWVAWuSmwKAHICrNvzTDCQz61ZCbmcigVYzC4"]
